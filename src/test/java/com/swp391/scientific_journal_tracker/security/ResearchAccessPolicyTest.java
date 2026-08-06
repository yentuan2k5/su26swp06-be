package com.swp391.scientific_journal_tracker.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ResearchAccessPolicyTest {

    private final ResearchAccessPolicy researchAccessPolicy = new ResearchAccessPolicy();

    @Test
    void lecturerReceivesBasicAccess() {
        assertEquals(
                ResearchAccessLevel.BASIC,
                researchAccessPolicy.resolve(authenticationWithRole("ROLE_LECTURER")));
    }

    @Test
    void researcherAndAdminReceiveFullAccess() {
        assertEquals(
                ResearchAccessLevel.FULL,
                researchAccessPolicy.resolve(authenticationWithRole("ROLE_RESEARCHER")));
        assertEquals(
                ResearchAccessLevel.FULL,
                researchAccessPolicy.resolve(authenticationWithRole("ROLE_ADMIN")));
    }

    @Test
    void studentIsDeniedInsteadOfReceivingFullAccess() {
        assertThrows(
                AccessDeniedException.class,
                () -> researchAccessPolicy.resolve(authenticationWithRole("ROLE_STUDENT")));
    }

    private UsernamePasswordAuthenticationToken authenticationWithRole(String role) {
        return new UsernamePasswordAuthenticationToken(
                "user",
                null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
