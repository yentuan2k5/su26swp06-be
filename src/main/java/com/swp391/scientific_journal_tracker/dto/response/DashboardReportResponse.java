package com.swp391.scientific_journal_tracker.dto.response;

import com.swp391.scientific_journal_tracker.entity.DashboardReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReportResponse {
    private Long dashboardReportId;
    private String title;
    private String content;
    private LocalDateTime generatedAt;
    private UserResponse user;
    private DashboardReportChartsResponse charts;

    public static DashboardReportResponse fromEntity(DashboardReport report) {
        return fromEntity(report, null);
    }

    public static DashboardReportResponse fromEntity(
            DashboardReport report,
            DashboardReportChartsResponse charts) {
        UserResponse userResponse = null;

        if (report.getUser() != null) {
            userResponse = UserResponse.builder()
                    .userId(report.getUser().getUserId())
                    .username(report.getUser().getUsername())
                    .email(report.getUser().getEmail())
                    .role(report.getUser().getRole().name())
                    .build();
        }

        return new DashboardReportResponse(
                report.getDashboardReportId(),
                report.getTitle(),
                report.getContent(),
                report.getGeneratedAt(),
                userResponse,
                charts);
    }
}
