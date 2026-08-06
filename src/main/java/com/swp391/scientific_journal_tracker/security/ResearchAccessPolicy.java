package com.swp391.scientific_journal_tracker.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Chuẩn hóa việc suy ra mức truy cập phân tích từ role hiện tại.
 *
 * Controller và service không nhận access level từ client. BASIC/FULL luôn
 * được suy ra từ authority đã được JWT filter nạp từ database cho request.
 */
@Component
public class ResearchAccessPolicy {

    public ResearchAccessLevel resolve(Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_LECTURER")) {
            return ResearchAccessLevel.BASIC;
        }

        if (hasAuthority(authentication, "ROLE_RESEARCHER")
                || hasAuthority(authentication, "ROLE_ADMIN")) {
            return ResearchAccessLevel.FULL;
        }

        throw new AccessDeniedException("Không có quyền truy cập chức năng phân tích");
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
