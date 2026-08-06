package com.swp391.scientific_journal_tracker.security;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Fixed-window rate limit nhỏ, tập trung vào endpoint dễ bị lạm dụng.
 * Bộ đếm nằm trong memory của instance hiện tại; với nhiều instance cần thay
 * bằng Redis hoặc rate limit tại gateway để có giới hạn phân tán.
 */
@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong();

    @Value("${security.rate-limit.login.max-requests:5}")
    private int loginMaxRequests;

    @Value("${security.rate-limit.login.window-seconds:60}")
    private long loginWindowSeconds;

    @Value("${security.rate-limit.password-reset.max-requests:3}")
    private int passwordResetMaxRequests;

    @Value("${security.rate-limit.password-reset.window-seconds:900}")
    private long passwordResetWindowSeconds;

    @Value("${security.rate-limit.backfill.max-requests:1}")
    private int backfillMaxRequests;

    @Value("${security.rate-limit.backfill.window-seconds:300}")
    private long backfillWindowSeconds;

    @Value("${security.rate-limit.sync.max-requests:1}")
    private int syncMaxRequests;

    @Value("${security.rate-limit.sync.window-seconds:60}")
    private long syncWindowSeconds;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolveRule(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String bucketKey = rule.name() + ":" + resolveClientKey(request);
        WindowCounter counter = counters.compute(bucketKey, (ignored, current) -> {
            if (current == null || current.expiresAtMillis() <= now) {
                return new WindowCounter(now + rule.windowMillis(), 1);
            }

            return new WindowCounter(current.expiresAtMillis(), current.requestCount() + 1);
        });

        cleanupExpiredCounters(now);

        if (counter.requestCount() > rule.maxRequests()) {
            long retryAfterSeconds = Math.max(1,
                    (long) Math.ceil((counter.expiresAtMillis() - now) / 1000.0));
            writeRateLimitResponse(response, retryAfterSeconds);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        return switch (request.getServletPath()) {
            case "/api/auth/login", "/api/auth/register" -> new RateLimitRule(
                    "auth", safeMax(loginMaxRequests), safeWindow(loginWindowSeconds));
            case "/api/auth/forgot-password", "/api/auth/reset-password" -> new RateLimitRule(
                    "password-reset", safeMax(passwordResetMaxRequests), safeWindow(passwordResetWindowSeconds));
            case "/api/admin/sync/backfill" -> new RateLimitRule(
                    "backfill", safeMax(backfillMaxRequests), safeWindow(backfillWindowSeconds));
            case "/api/admin/sync" -> new RateLimitRule(
                    "sync", safeMax(syncMaxRequests), safeWindow(syncWindowSeconds));
            default -> null;
        };
    }

    private String resolveClientKey(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private void cleanupExpiredCounters(long now) {
        long cleanupIntervalMillis = 60_000;
        long previousCleanup = lastCleanupAt.get();
        if (now - previousCleanup < cleanupIntervalMillis
                || !lastCleanupAt.compareAndSet(previousCleanup, now)) {
            return;
        }

        counters.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private void writeRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("{\"timestamp\":\"" + Instant.now()
                + "\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Bạn thao tác quá nhanh. Vui lòng thử lại sau.\"}");
    }

    private int safeMax(int maxRequests) {
        return Math.max(1, maxRequests);
    }

    private long safeWindow(long windowSeconds) {
        return Math.max(1, windowSeconds) * 1000;
    }

    private record RateLimitRule(String name, int maxRequests, long windowMillis) {
    }

    private record WindowCounter(long expiresAtMillis, int requestCount) {
    }
}
