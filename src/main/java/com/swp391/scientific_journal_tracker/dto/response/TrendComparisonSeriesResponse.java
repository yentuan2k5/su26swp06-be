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
    private long previousCount;
    private long recentCount;
    private double trendScore;
    private String trendType;
    private boolean sufficientData;
    private List<TrendResponse> yearlyData;

    /**
     * Chuỗi tăng trưởng theo từng năm của series. growthRate dùng dạng thập
     * phân, ví dụ 0.25 tương ứng tăng 25% so với năm trước.
     */
    private List<TrendGrowthPointResponse> yearlyGrowthData;

    /** Constructor cu de giu tuong thich khi client chi dung yearlyData. */
    public TrendComparisonSeriesResponse(
            String name,
            long totalPapers,
            double growthRate,
            List<TrendResponse> yearlyData) {
        this.name = name;
        this.totalPapers = totalPapers;
        this.growthRate = growthRate;
        this.yearlyData = yearlyData;
    }
}
