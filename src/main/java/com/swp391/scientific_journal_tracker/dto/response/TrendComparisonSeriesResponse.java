package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một chuỗi dữ liệu của keyword hoặc topic trong chức năng Compare Trends.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendComparisonSeriesResponse {
    private String name;
    private long totalPapers;
    private double growthRate;
    private List<TrendResponse> yearlyData;
}
