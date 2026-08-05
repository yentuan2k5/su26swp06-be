package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Các chỉ số nghiên cứu nâng cao dành cho Lecturer, Researcher và Admin. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsResponse {
    private long totalCitations;
    private double averageCitationsPerPaper;
    private long highImpactPaperCount;
    private int latestCompleteYear;
    private long latestCompleteYearPaperCount;
    private int previousCompleteYear;
    private long previousCompleteYearPaperCount;
    private double publicationGrowthRate;
    private List<TopKeywordResponse> topTrendingKeywords;
    private List<TopTopicResponse> topTrendingTopics;
}
