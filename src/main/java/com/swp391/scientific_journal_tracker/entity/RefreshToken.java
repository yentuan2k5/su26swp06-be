package com.swp391.scientific_journal_tracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "RefreshTokens")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "RefreshTokenId", nullable = false, unique = true)
    private Long refreshTokenId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;
    @Column(name = "Token", nullable = false, unique = true, columnDefinition = "VARCHAR(512) COLLATE utf8mb4_unicode_ci")
    private String token;
    @Column(name = "ExpiredAt", nullable = false)
    private LocalDateTime expiredAt;
    @Column(name = "CreatedAt", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();
}