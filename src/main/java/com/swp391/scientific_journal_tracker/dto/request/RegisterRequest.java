package com.swp391.scientific_journal_tracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(max = 150, message = "Username tối đa 150 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email phải đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 255, message = "Mật khẩu phải từ 8 đến 255 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    @Size(max = 255, message = "Xác nhận mật khẩu không hợp lệ")
    private String confirmPassword;

    @NotBlank(message = "Vai trò không được để trống ")
    @Pattern(
            regexp = "(?i)^(?:STUDENT|LECTURER|RESEARCHER)$",
            message = "Chỉ được đăng ký role STUDENT, LECTURER hoặc RESEARCHER")
    private String role;
}
