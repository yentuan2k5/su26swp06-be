package com.swp391.scientific_journal_tracker.controller;

import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import com.swp391.scientific_journal_tracker.service.DashboardReportService;
import com.swp391.scientific_journal_tracker.service.SyncService;
import com.swp391.scientific_journal_tracker.service.UserService;
import com.swp391.scientific_journal_tracker.dto.request.UpdateUserRoleRequest;
import com.swp391.scientific_journal_tracker.dto.response.DashboardReportResponse;
import com.swp391.scientific_journal_tracker.dto.response.UserResponse;
import jakarta.validation.Valid;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN mới được gọi
public class AdminController {

    private final DashboardReportService dashboardReportService;
    private final UserService userService;
    private final SyncService syncService;
    private final SyncLogRepository syncLogRepository;

    /**
     * POST /api/admin/sync
     * Trigger sync thủ công — Admin dùng khi muốn sync ngay, không chờ scheduler
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncLogResponse> triggerSync() {
        SyncLogResponse result = syncService.syncFromOpenAlex();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/sync/logs
     * Xem lịch sử các lần sync
     */
    @GetMapping("/sync/logs")
    public ResponseEntity<List<SyncLogResponse>> getSyncLogs() {
        List<SyncLogResponse> logs = syncLogRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(SyncLogResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/admin/sync/logs/{id}
     * Xem chi tiết 1 lần sync
     */
    @GetMapping("/sync/logs/{id}")
    public ResponseEntity<SyncLogResponse> getSyncLog(@PathVariable long id) {
        return syncLogRepository.findById(id)
                .map(SyncLogResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(userService.searchUsers(keyword));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<List<DashboardReportResponse>> getAllReports() {
        return ResponseEntity.ok(dashboardReportService.getAllReportsForAdmin());
    }
}
