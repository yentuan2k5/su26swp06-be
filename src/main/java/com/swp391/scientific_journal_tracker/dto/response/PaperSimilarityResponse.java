package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Độ tương đồng metadata giữa một cặp paper. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperSimilarityResponse {
    private Long firstPaperId;
    private Long secondPaperId;
    private double keywordSimilarity;
    private double topicSimilarity;
    private double overallSimilarity;
}
