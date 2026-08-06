package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(max = 150, message = "Username tối đa 150 ký tự")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(max = 255, message = "Mật khẩu không hợp lệ")
    private String password;
}
