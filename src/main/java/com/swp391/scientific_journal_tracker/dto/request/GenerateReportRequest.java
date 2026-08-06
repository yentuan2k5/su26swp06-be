package com.swp391.scientific_journal_tracker.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerateReportRequest {
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
    private String title;

    @Size(max = 150, message = "Keyword tối đa 150 ký tự")
    private String keyword;

    @Size(max = 150, message = "Topic tối đa 150 ký tự")
    private String topic;

    /*
     * Danh sách section mà frontend muốn đưa vào report.
     * Ví dụ:
     * OVERALL_STATISTICS, PAPERS_BY_YEAR, TOP_KEYWORDS, TOP_JOURNALS,
     * TOP_CITED_PAPERS, KEYWORD_TREND, TOPIC_TREND, TOP_TRENDING_TOPICS.
     *
     * Nếu không truyền, backend giữ hành vi cũ và sinh đầy đủ các section.
     */
    @Size(max = 8, message = "Tối đa 8 section cho một report")
    private List<
            @NotBlank(message = "Section không được để trống")
            @Pattern(
                    regexp = "(?i)^(?:OVERALL_STATISTICS|PAPERS_BY_YEAR|TOP_KEYWORDS|TOP_JOURNALS|TOP_CITED_PAPERS|KEYWORD_TREND|TOPIC_TREND|TOP_TRENDING_TOPICS)$",
                    message = "Section report không hợp lệ") String> sections;

    @Min(value = 1, message = "Time horizon tối thiểu là 1 năm")
    @Max(value = 30, message = "Time horizon tối đa là 30 năm")
    private Integer timeHorizonYears;

    @Size(max = 20, message = "Định dạng report tối đa 20 ký tự")
    private String format;
}
