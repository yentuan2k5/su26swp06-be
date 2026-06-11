package com.swp391.scientific_journal_tracker.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class AuthResponse {
    private String refreshToken;
    private String token;
    private UserResponse user;
}
