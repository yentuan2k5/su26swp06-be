package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReportChartsResponse {
    private List<DashboardChartItemResponse> papersByYear;
    private List<DashboardChartItemResponse> topKeywords;
    private List<DashboardChartItemResponse> topJournals;
    private List<TrendResponse> keywordTrend;
    private List<TrendResponse> topicTrend;
    private List<TopTopicResponse> topTrendingTopics;
}
