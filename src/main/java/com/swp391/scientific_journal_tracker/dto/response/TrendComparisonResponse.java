package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả so sánh xu hướng nhiều keyword hoặc topic trong cùng một khoảng năm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendComparisonResponse {
    private String type;
    private int fromYear;
    private int toYear;
    private List<TrendComparisonSeriesResponse> series;
}
