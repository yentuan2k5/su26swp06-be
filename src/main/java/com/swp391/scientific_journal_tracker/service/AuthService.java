package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.entity.RefreshToken;
import com.swp391.scientific_journal_tracker.dto.request.LoginRequest;
import com.swp391.scientific_journal_tracker.dto.request.RegisterRequest;
import com.swp391.scientific_journal_tracker.dto.request.ResetPasswordRequest;
import com.swp391.scientific_journal_tracker.dto.response.AuthResponse;
import com.swp391.scientific_journal_tracker.dto.response.UserResponse;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.repository.RefreshTokenRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import com.swp391.scientific_journal_tracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Objects;
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

    // ── ĐĂNG KÝ ──────────────────────────────────────────────
    public UserResponse register(RegisterRequest req) {
        // Kiểm tra xem UserName đã tồn tại chưa
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username đã được sử dụng");
        }
        // Kiểm tra xem email đẫ được sử dụng chưa
        if (userRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email đã được sử dụng");
        // Kiểm tra passconfirm có giống password nhập ở trên không
        if (!req.getPassword().equals(req.getConfirmPassword()))
            throw new RuntimeException("Mật khẩu xác nhận không khớp");

        // Chặn không cho phép tạo tài khoản với vai trò admin
        User.Role role;

        try {
            role = User.Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Vai trò không hợp lệ");
        }

        if (role == User.Role.ADMIN) {
            throw new RuntimeException("Không được đăng ký tài khoản ADMIN");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .provider("local")
                .role(role)
                .build();

        User savedUser = userRepo.save(Objects.requireNonNull(user));
        return toUserResponse(savedUser);
    }

    // ── ĐĂNG NHẬP ────────────────────────────────────────────
    public AuthResponse login(LoginRequest req) {

        User user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Username hoặc mật khẩu không đúng"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new RuntimeException("Email hoặc mật khẩu không đúng");

        String token = jwtService.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
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

        String resetLink = frontendUrl + "reset-password?token=" + token;

        emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
    }

    // ── ĐẶT LẠI MẬT KHẨU ────────────────────────────────────
    public void resetPassword(ResetPasswordRequest req) {

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        User user = userRepo.findByResetPasswordToken(req.getToken())
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token đã hết hạn, vui lòng yêu cầu lại");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetTokenExpiry(null);

        userRepo.save(user);
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
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        if (saved.getExpiredAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(saved);
            throw new RuntimeException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = saved.getUser();
        String newAccessToken = jwtService.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(newAccessToken)
                .user(toUserResponse(user))
                .build();
    }

    // ── LOGOUT ───────────────────────────────────────────────
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }
}
