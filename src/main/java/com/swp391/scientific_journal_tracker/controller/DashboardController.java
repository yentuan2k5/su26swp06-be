package com.swp391.scientific_journal_tracker.controller;

import com.swp391.scientific_journal_tracker.dto.response.DashboardAnalyticsResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardHomeResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardOverviewResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardOperationsResponse;
import com.swp391.scientific_journal_tracker.service.DashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Dashboard tổng hợp theo role: mọi người có overview, nhóm học thuật có
     * analytics và chỉ Admin nhận số liệu vận hành hệ thống.
     */
    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('STUDENT', 'LECTURER', 'RESEARCHER', 'ADMIN')")
    public DashboardHomeResponse getHome(Authentication authentication) {
        return dashboardService.getHome(authentication.getName());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('STUDENT', 'LECTURER', 'RESEARCHER', 'ADMIN')")
    public DashboardOverviewResponse getSummary() {
        return dashboardService.getSummary();
    }

    /**
     * Chỉ số nghiên cứu nâng cao như citation impact, tăng trưởng công bố theo
     * năm hoàn chỉnh gần nhất và danh sách keyword/topic đang tăng trưởng.
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('LECTURER', 'RESEARCHER', 'ADMIN')")
    public DashboardAnalyticsResponse getAnalytics() {
        return dashboardService.getAnalytics();
    }

    /** Các chỉ số vận hành OpenAlex và lịch sử đồng bộ, chỉ dành cho Admin. */
    @GetMapping("/operations")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardOperationsResponse getOperations() {
        return dashboardService.getOperations();
    }
}
