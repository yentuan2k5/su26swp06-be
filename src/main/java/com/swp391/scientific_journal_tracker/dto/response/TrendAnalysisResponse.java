package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả phân tích xu hướng của một keyword hoặc topic.
 *
 * yearlyData là số paper nền để đối chiếu. yearlyGrowthData là dữ liệu đã
 * tính toán để vẽ biểu đồ trend theo tỷ lệ tăng/giảm từng năm. Kết luận xu
 * hướng tổng thể được suy ra từ hai giai đoạn liên tiếp: previous và recent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysisResponse {
    private String type;
    private String name;
    private int previousFromYear;
    private int previousToYear;
    private int recentFromYear;
    private int recentToYear;
    private long previousCount;
    private long recentCount;
    private long totalPapers;
    private double growthRate;
    private double trendScore;
    private String trendType;
    private boolean sufficientData;
    private List<TrendResponse> yearlyData;
    private List<TrendGrowthPointResponse> yearlyGrowthData;
}
