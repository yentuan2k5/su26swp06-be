package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Các câu nổi bật được trích xuất theo rule từ abstract của một paper.
 * Đây là heuristic dựa trên cụm từ học thuật, không phải kết luận do mô hình
 * AI hoặc phân tích toàn văn.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbstractAnalysisResponse {
    private String source;
    private List<String> objectiveHighlights;
    private List<String> problemHighlights;
    private List<String> methodHighlights;
    private List<String> resultHighlights;
}
