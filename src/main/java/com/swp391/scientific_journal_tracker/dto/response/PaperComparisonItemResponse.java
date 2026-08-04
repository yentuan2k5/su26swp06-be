package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Metadata của một paper trong kết quả so sánh. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperComparisonItemResponse {
    private PaperResponse paper;
    private double citationsPerYear;
    private List<String> uniqueKeywords;
    private List<String> uniqueTopics;
}
