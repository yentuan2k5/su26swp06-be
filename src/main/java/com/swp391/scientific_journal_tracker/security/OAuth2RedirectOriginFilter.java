package com.swp391.scientific_journal_tracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * Khi FE bắt đầu luồng Google OAuth2 (GET /oauth2/authorization/google),
 * nó có thể đính kèm query param redirect_origin = origin (scheme + host)
 * của domain frontend đang gọi (ví dụ domain preview Vercel).
 *
 * Filter này chặn request đó lại TRƯỚC khi Spring Security redirect sang
 * Google, lưu origin đó vào 1 cookie ngắn hạn (oauth2_redirect_origin).
 * OAuth2SuccessHandler sẽ đọc lại cookie này sau khi Google callback về,
 * để biết redirect người dùng quay lại đúng domain frontend nào.
 *
 * Chỉ chấp nhận origin nằm trong whitelist (localhost hoặc *.vercel.app)
 * để tránh bị lợi dụng làm open-redirect.
 */
@Component
public class OAuth2RedirectOriginFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "oauth2_redirect_origin";

    private static final Pattern ALLOWED_ORIGIN_PATTERN = Pattern.compile(
            "^https?://(localhost(:\\d+)?|[a-z0-9-]+\\.vercel\\.app)$",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if ("/oauth2/authorization/google".equals(request.getServletPath())) {
            String redirectOrigin = request.getParameter("redirect_origin");

            if (isAllowedOrigin(redirectOrigin)) {
                Cookie cookie = new Cookie(COOKIE_NAME, redirectOrigin);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setMaxAge(5 * 60); // 5 phút là đủ cho cả luồng Google login
                cookie.setSecure(true);
                cookie.setAttribute("SameSite", "None"); // cần None vì có redirect cross-site qua Google
                response.addCookie(cookie);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) return false;

        try {
            URI uri = URI.create(origin);
            String normalized = uri.getScheme() + "://" + uri.getAuthority();
            return ALLOWED_ORIGIN_PATTERN.matcher(normalized).matches();
        } catch (Exception e) {
            return false;
        }
    }
}
