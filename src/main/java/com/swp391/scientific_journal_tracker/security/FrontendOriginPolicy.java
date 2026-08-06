package com.swp391.scientific_journal_tracker.security;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Whitelist origin frontend dùng chung cho CORS và OAuth2 redirect.
 * Không dùng wildcard Vercel khi cho phép credentials để tránh website Vercel
 * không thuộc dự án có thể gọi API thay mặt người dùng.
 */
@Component
public class FrontendOriginPolicy {

    @Value("${app.frontend-url:http://localhost:5173/}")
    private String frontendUrl;

    @Value("${app.cors.allowed-origins:}")
    private String additionalAllowedOrigins;

    public List<String> getAllowedOrigins() {
        Set<String> origins = new LinkedHashSet<>();
        addNormalizedOrigin(origins, frontendUrl);

        if (additionalAllowedOrigins != null) {
            for (String origin : additionalAllowedOrigins.split(",")) {
                addNormalizedOrigin(origins, origin);
            }
        }

        return List.copyOf(origins);
    }

    public boolean isAllowedOrigin(String origin) {
        String normalizedOrigin = normalizeOrigin(origin);
        return normalizedOrigin != null && getAllowedOrigins().contains(normalizedOrigin);
    }

    private void addNormalizedOrigin(Set<String> origins, String origin) {
        String normalizedOrigin = normalizeOrigin(origin);
        if (normalizedOrigin != null) {
            origins.add(normalizedOrigin);
        }
    }

    private String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(origin.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                return null;
            }

            return uri.getScheme().toLowerCase() + "://" + uri.getAuthority().toLowerCase();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
