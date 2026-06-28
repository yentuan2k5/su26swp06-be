package com.swp391.scientific_journal_tracker.security;

import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

import com.swp391.scientific_journal_tracker.entity.RefreshToken;
import com.swp391.scientific_journal_tracker.repository.RefreshTokenRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${jwt.refresh-expiration-ms:86400000}")
    private long refreshTokenExpirationMs;

    @SuppressWarnings("null")
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(email)
                            .email(email)
                            .googleId(googleId)
                            .provider("google")
                            .role(User.Role.STUDENT)
                            .build();

                    return userRepository.save(newUser);
                });

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setProvider("google");
            userRepository.save(user);
        }
        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiredAt(
                LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));

        refreshTokenRepository.save(refreshTokenEntity);

<<<<<<< Updated upstream
        getRedirectStrategy().sendRedirect(request, response,
                frontendUrl + "oauth2/callback?token=" + token);
=======
        String redirectBase = resolveRedirectOrigin(request);

        String redirectUrl = redirectBase + "oauth2/callback"
                + "?token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Ưu tiên domain mà FE đã gửi kèm lúc bắt đầu luồng Google login
     * (lưu trong cookie bởi OAuth2RedirectOriginFilter). Nếu không có
     * (hoặc cookie không hợp lệ), fallback về domain mặc định trong
     * app.frontend-url.
     */
    private String resolveRedirectOrigin(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (OAuth2RedirectOriginFilter.COOKIE_NAME.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue().replaceAll("/$", "") + "/";
                }
            }
        }

        return frontendUrl.replaceAll("/$", "") + "/";
>>>>>>> Stashed changes
    }

}
