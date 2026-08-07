package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một điểm dữ liệu đã tính toán cho biểu đồ xu hướng.
 *
 * growthRate so sánh số paper của năm hiện tại với năm liền trước. Client
 * nhân growthRate với 100 khi hiển thị theo phần trăm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendGrowthPointResponse {
    private Integer year;
    private long previousPaperCount;
    private long paperCount;
    private double growthRate;
    private double trendScore;
}
