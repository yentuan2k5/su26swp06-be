package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopKeywordResponse {
    private String keyword;
    private Long paperCount;
    private double growthRate;
    private long totalPapers;
    private double score;
    private String trendType;
}
