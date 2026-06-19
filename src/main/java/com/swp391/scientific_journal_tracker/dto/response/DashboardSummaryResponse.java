package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalPapers;
    private long totalJournals;
    private long totalKeywords;
    private long openAlexPapers;
    private long successfulSyncs;
    private long failedSyncs;
    private List<DashboardChartItemResponse> papersByYear;
    private List<DashboardChartItemResponse> topKeywords;
    private List<DashboardChartItemResponse> topJournals;
    private List<PaperResponse> topCitedPapers;
    private SyncLogResponse latestSyncLog;
}
