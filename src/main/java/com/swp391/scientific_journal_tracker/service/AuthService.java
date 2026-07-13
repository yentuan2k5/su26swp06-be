package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.entity.RefreshToken;
import com.swp391.scientific_journal_tracker.dto.request.LoginRequest;
import com.swp391.scientific_journal_tracker.dto.request.RegisterRequest;
import com.swp391.scientific_journal_tracker.dto.request.ResetPasswordRequest;
import com.swp391.scientific_journal_tracker.dto.response.AuthResponse;
import com.swp391.scientific_journal_tracker.dto.response.UserResponse;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.exception.DuplicateResourceException;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.RefreshTokenRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import com.swp391.scientific_journal_tracker.security.JwtService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.reset-token-expiry-minutes:15}")
    private int resetTokenExpiryMinutes;
    @Value("${jwt.refresh-expiration-ms:86400000}")
    private long refreshTokenExpirationMs;

    // ── ĐĂNG KÝ ──────────────────────────────────────────────
    public UserResponse register(RegisterRequest req) {
        // check username đã tồn tại chưa
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("Username đã được sử dụng");
        }
        // check email đã được sử dụng chưa
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        // check confirm mật khẩu đã giống chưa
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        User.Role role;

        try {
            role = User.Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Vai trò không hợp lệ");
        }

        if (role == User.Role.ADMIN) {
            throw new BadRequestException("Không được đăng ký tài khoản ADMIN");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .provider("local")
                .role(role)
                .build();

        User savedUser = userRepo.save(java.util.Objects.requireNonNull(user));
        return toUserResponse(savedUser);

    }

    // ── ĐĂNG NHẬP ────────────────────────────────────────────
    @Transactional
    public AuthResponse login(LoginRequest req) {

        User user = userRepo.findByEmailOrUsername(req.getUsername(), req.getUsername())
                .orElseThrow(() -> new BadRequestException("Username hoặc mật khẩu không đúng"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Username hoặc mật khẩu không đúng");
        }

        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUser(user);
        tokenEntity.setToken(refreshToken);
        tokenEntity.setExpiredAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));

        refreshTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(toUserResponse(user))
                .build();
    }

    // ── QUÊN MẬT KHẨU ────────────────────────────────────────
    public void forgotPassword(String identifier) {
        User user = userRepo.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!"local".equalsIgnoreCase(user.getProvider())) {
            throw new RuntimeException("Tài khoản này đăng nhập bằng Google, vui lòng dùng Google để đăng nhập");
        }

        String token = UUID.randomUUID().toString();

        user.setResetPasswordToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes));

        userRepo.save(user);

        // String resetLink = frontendUrl + "reset-password?token=" + token;

        // emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    // ── ĐẶT LẠI MẬT KHẨU ────────────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {

        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new BadRequestException("Token đặt lại mật khẩu không được để trống");
        }

        if (req.getNewPassword() == null
                || req.getConfirmPassword() == null
                || !req.getNewPassword().equals(req.getConfirmPassword())) {

            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        User user = userRepo.findByResetPasswordToken(req.getToken())
                .orElseThrow(() -> new BadRequestException("Token không hợp lệ"));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {

            /*
             * Xóa token reset đã hết hạn để không tiếp tục lưu dữ liệu rác.
             */
            user.setResetPasswordToken(null);
            user.setResetTokenExpiry(null);
            userRepo.save(user);

            throw new BadRequestException(
                    "Token đã hết hạn, vui lòng yêu cầu lại");
        }

        /*
         * Cập nhật mật khẩu mới.
         */
        user.setPasswordHash(
                passwordEncoder.encode(req.getNewPassword()));

        /*
         * Reset token chỉ được sử dụng một lần.
         */
        user.setResetPasswordToken(null);
        user.setResetTokenExpiry(null);

        userRepo.save(user);

        /*
         * Thu hồi tất cả refresh token cũ của tài khoản.
         * Người dùng phải đăng nhập lại bằng mật khẩu mới.
         */
        refreshTokenRepository.deleteAllByUser(user);
    }

    // ── HELPER ───────────────────────────────────────────────
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    // ── REFRESH TOKEN ────────────────────────────────────────
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException(
                    "Refresh token không được để trống");
        }
        /*
         * Kiểm tra JWT trước:
         * - Đúng chữ ký không?
         * - Hết hạn chưa?
         * - Có tokenType = REFRESH không?
         */
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BadRequestException(
                    "Refresh token không hợp lệ hoặc đã hết hạn");
        }

        /*
         * Sau khi JWT hợp lệ mới kiểm tra token
         * có đang tồn tại trong database không.
         *
         * Điều này cho phép logout bằng cách xóa token khỏi database.
         */
        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException(
                        "Refresh token không tồn tại hoặc đã bị thu hồi"));

        /*
         * Kiểm tra thêm thời gian hết hạn được lưu trong database.
         */
        if (savedToken.getExpiredAt() == null
                || savedToken.getExpiredAt().isBefore(LocalDateTime.now())) {

            refreshTokenRepository.delete(savedToken);

            throw new BadRequestException(
                    "Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = savedToken.getUser();

        // Chỉ cấp access token mới
        String newAccessToken = jwtService.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .user(toUserResponse(user))
                .build();
    }

    // ── LOGOUT ───────────────────────────────────────────────
    @Transactional
    public void logout(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token không được để trống");
        }

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    public UserResponse getCurrentUser(String username) {

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        return toUserResponse(user);
    }
}
