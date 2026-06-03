package com.swp391.scientific_journal_tracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Column(name = "Name", nullable = false, columnDefinition = "VARCHAR(100) COLLATE utf8mb4_unicode_ci")
    private String name;
    @Column(name = "Email", nullable = false, unique = true, columnDefinition = "VARCHAR(150) COLLATE utf8mb4_unicode_ci")
    private String email;
    @Column(name = "Password", nullable = false, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false)
    private Role role;

    public enum Role {
        ADMIN,
        LECTURER,
        STUDENT,
        RESEARCHER
    }
}
