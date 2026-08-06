package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Khả năng mà frontend có thể hiển thị trên Dashboard theo role hiện tại.
 * Các API chức năng riêng vẫn tự kiểm tra quyền, không dựa riêng vào DTO này.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCapabilitiesResponse {
    private boolean canViewResearchAnalytics;
    private boolean canViewEmergingTopics;
    private boolean canGenerateBasicReport;
    private boolean canGenerateAdvancedReport;
    private boolean canCompareTrends;
    private boolean canComparePapers;
    private String mindMapAccess;
    private boolean canManageSystem;
}
