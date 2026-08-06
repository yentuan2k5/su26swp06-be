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
 * Chỉ chấp nhận origin được cấu hình rõ trong whitelist dùng chung với CORS
 * để tránh bị lợi dụng làm open-redirect.
 */
@Component
public class OAuth2RedirectOriginFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "oauth2_redirect_origin";

    private final FrontendOriginPolicy frontendOriginPolicy;

    public OAuth2RedirectOriginFilter(FrontendOriginPolicy frontendOriginPolicy) {
        this.frontendOriginPolicy = frontendOriginPolicy;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if ("/api/oauth2/authorization/google".equals(request.getServletPath())) {
            String redirectOrigin = request.getParameter("redirect_origin");

            if (frontendOriginPolicy.isAllowedOrigin(redirectOrigin)) {
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
}
