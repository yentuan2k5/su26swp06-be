package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotBlank(message = "Role không được để trống")
    @Pattern(
            regexp = "(?i)^(?:ADMIN|STUDENT|LECTURER|RESEARCHER)$",
            message = "Role không hợp lệ")
    private String role;
}
