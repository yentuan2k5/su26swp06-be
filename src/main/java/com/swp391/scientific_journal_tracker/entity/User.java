package com.swp391.scientific_journal_tracker.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Entity
@Table(name = "Users") // Table name in plural form
@AllArgsConstructor // Constructor with all fields
@NoArgsConstructor // Default constructor
@Data // Getters, Setters, toString, equals, and hashCode
public class User {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "UserId", nullable = false, unique = true)
    private Long userId;
    @Column(name = "UserName", nullable = false, unique = true, columnDefinition = "VARCHAR(150) COLLATE utf8mb4_unicode_ci")
    private String username;
    @Column(name = "Email", nullable = false, unique = true, columnDefinition = "VARCHAR(150) COLLATE utf8mb4_unicode_ci")
    private String email;
    @Column(name = "Password", nullable = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    private String passwordHash;
    @Builder.Default
    @Column(name = "Provider", nullable = false, columnDefinition = "VARCHAR(30) COLLATE utf8mb4_unicode_ci DEFAULT 'local'")
    private String provider = "local";

    @Column(name = "GoogleId", columnDefinition = "VARCHAR(100) COLLATE utf8mb4_unicode_ci")
    private String googleId;
    @Column
    private String resetPasswordToken; // token reset password
    @Column
    private LocalDateTime resetTokenExpiry; // hết hạn sau 15 phút
    @Builder.Default
    @Column(name = "CreatAt", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime creatAt = LocalDateTime.now();
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false)
    private Role role = Role.STUDENT; // Default role is STUDENT

    public enum Role {
        ADMIN,
        LECTURER,
        STUDENT,
        RESEARCHER
    }

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookmark> bookmarks = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "user_following_journals", joinColumns = @JoinColumn(name = "UserId"), inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "journalId"))
    private List<Journal> followingJournals = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "user_following_topics", joinColumns = @JoinColumn(name = "UserId"), inverseJoinColumns = @JoinColumn(name = "ResearchTopicId"))
    private List<ResearchTopic> followingTopics = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DashboardReport> dashboardReports = new ArrayList<>();
}