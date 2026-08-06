package com.swp391.scientific_journal_tracker.controller;

import com.swp391.scientific_journal_tracker.dto.request.GenerateReportRequest;
import com.swp391.scientific_journal_tracker.dto.response.DashboardReportResponse;
import com.swp391.scientific_journal_tracker.service.DashboardReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('LECTURER', 'RESEARCHER', 'ADMIN')")
public class DashboardReportController {

    private final DashboardReportService dashboardReportService;

    @PostMapping("/generate")
    public ResponseEntity<DashboardReportResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(dashboardReportService.generateReport(request, authentication));
    }

    @GetMapping("/my")
    public ResponseEntity<List<DashboardReportResponse>> getMyReports(Authentication authentication) {
        return ResponseEntity.ok(dashboardReportService.getMyReports(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DashboardReportResponse> getReportDetail(
            @PathVariable @Positive(message = "reportId phải là số nguyên dương") Long id,
            Authentication authentication) {
        return ResponseEntity.ok(dashboardReportService.getMyReportDetail(id, authentication));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DashboardReportResponse>> searchMyReports(
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        return ResponseEntity.ok(dashboardReportService.searchMyReports(keyword, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable @Positive(message = "reportId phải là số nguyên dương") Long id,
            Authentication authentication) {
        dashboardReportService.deleteMyReport(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
