package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerateReportRequest {
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
    private String title;

    private String keyword;

    private String topic;
}