package com.swp391.scientific_journal_tracker.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FrontendOriginPolicyTest {

    @Test
    void allowsOnlyConfiguredExactOrigins() {
        FrontendOriginPolicy policy = new FrontendOriginPolicy();
        ReflectionTestUtils.setField(policy, "frontendUrl", "https://science-trend.vercel.app/");
        ReflectionTestUtils.setField(policy, "additionalAllowedOrigins", "http://localhost:5173");

        assertEquals(List.of("https://science-trend.vercel.app", "http://localhost:5173"),
                policy.getAllowedOrigins());
        assertTrue(policy.isAllowedOrigin("https://science-trend.vercel.app"));
        assertFalse(policy.isAllowedOrigin("https://another-project.vercel.app"));
        assertFalse(policy.isAllowedOrigin("https://science-trend.vercel.app/unsafe-path"));
    }
}
