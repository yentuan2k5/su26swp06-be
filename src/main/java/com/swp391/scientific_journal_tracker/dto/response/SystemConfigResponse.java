package com.swp391.scientific_journal_tracker.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigResponse {
    private OpenAlexConfig openAlex;
    private TrendConfig trend;
    private List<ApiDataSourceConfig> dataSources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAlexConfig {
        private List<String> fieldIds;
        private int syncLimit;
        private int overlapDays;
        private int backfillMaxResultsPerConcept;
        private boolean apiKeyConfigured;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendConfig {
        private int minPapersThreshold;
        private int minPreviousPapersThreshold;
        private int minRecentPapersForEmerging;
        private List<String> excludedKeywords;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiDataSourceConfig {
        private Long id;
        private String name;
        private String baseUrl;
        private LocalDateTime lastSyncTime;
    }
}
