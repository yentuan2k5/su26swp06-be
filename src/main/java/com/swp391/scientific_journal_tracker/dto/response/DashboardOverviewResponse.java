package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dashboard cơ bản, không chứa số liệu vận hành hệ thống. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private long totalPapers;
    private long totalJournals;
    private long totalKeywords;
    private long totalTopics;
    private List<DashboardChartItemResponse> papersByYear;
    private List<DashboardChartItemResponse> topKeywords;
    private List<DashboardChartItemResponse> topJournals;
    private List<PaperResponse> topCitedPapers;
}
