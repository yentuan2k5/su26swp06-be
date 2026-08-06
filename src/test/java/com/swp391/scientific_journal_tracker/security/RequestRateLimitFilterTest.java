package com.swp391.scientific_journal_tracker.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;

class RequestRateLimitFilterTest {

    @Test
    void blocksLoginAfterConfiguredLimit() throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter();
        ReflectionTestUtils.setField(filter, "loginMaxRequests", 2);
        ReflectionTestUtils.setField(filter, "loginWindowSeconds", 60L);
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilter(loginRequest(), new MockHttpServletResponse(), filterChain);
        filter.doFilter(loginRequest(), new MockHttpServletResponse(), filterChain);

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), blockedResponse, filterChain);

        verify(filterChain, times(2)).doFilter(Mockito.any(), Mockito.any());
        assertEquals(429, blockedResponse.getStatus());
        assertEquals("60", blockedResponse.getHeader("Retry-After"));
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
