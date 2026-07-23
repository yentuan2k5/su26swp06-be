package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Data;

@Data
public class BackfillOpenAlexRequest {
    @NotNull(message = "fromYear không được để trống")
    @Min(value = 1900, message = "fromYear không hợp lệ")
    private Integer fromYear;

    @NotNull(message = "toYear không được để trống")
    @Min(value = 1900, message = "toYear không hợp lệ")
    private Integer toYear;

    private List<String> fieldIds;

    @Positive(message = "maxResults phải lớn hơn 0")
    private Integer maxResults;
}
