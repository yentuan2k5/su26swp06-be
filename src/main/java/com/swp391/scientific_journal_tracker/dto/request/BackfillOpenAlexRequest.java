package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotEmpty(message = "fieldIds không được để trống")
    private List<
            @NotBlank(message = "fieldId không được để trống")
            @Pattern(
                    regexp = "(?:\\d+|(?:https?://openalex\\.org/)?fields/\\d+)",
                    message = "fieldId phải là số hoặc OpenAlex field ID hợp lệ") String> fieldIds;

    @Positive(message = "maxResults phải lớn hơn 0")
    @Max(value = 5000, message = "maxResults tối đa là 5000")
    private Integer maxResults;

    @AssertTrue(message = "fromYear phải nhỏ hơn hoặc bằng toYear")
    public boolean isValidYearRange() {
        return fromYear == null || toYear == null || fromYear <= toYear;
    }
}
