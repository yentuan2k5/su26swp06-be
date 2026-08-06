package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Token không được để trống")
    @Size(max = 512, message = "Token không hợp lệ")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 255, message = "Mật khẩu phải từ 8 đến 255 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    @Size(max = 255, message = "Xác nhận mật khẩu không hợp lệ")
    private String confirmPassword;
}
