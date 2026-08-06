package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dữ liệu Dashboard đã được đóng gói theo role của người dùng đăng nhập.
 * Analytics và operations có thể null khi role không có quyền xem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardHomeResponse {
    private String role;
    private DashboardOverviewResponse overview;
    private DashboardAnalyticsResponse analytics;
    private DashboardOperationsResponse operations;
    private DashboardCapabilitiesResponse capabilities;
}
