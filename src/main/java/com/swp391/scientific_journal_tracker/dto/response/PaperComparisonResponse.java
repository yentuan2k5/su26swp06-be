package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response phục vụ bảng so sánh metadata của từ hai đến bốn paper. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperComparisonResponse {
    private List<PaperComparisonItemResponse> papers;
    private Long newestPaperId;
    private Long mostCitedPaperId;
    private Long highestCitationsPerYearPaperId;
    private List<String> commonKeywords;
    private List<String> commonTopics;
    private List<PaperSimilarityResponse> similarities;
}
