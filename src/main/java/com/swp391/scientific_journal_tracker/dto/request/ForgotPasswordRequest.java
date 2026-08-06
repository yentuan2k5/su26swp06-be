package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Vui lòng nhập username hoặc email")
    @Size(max = 150, message = "Username hoặc email tối đa 150 ký tự")
    private String identifier;
}
