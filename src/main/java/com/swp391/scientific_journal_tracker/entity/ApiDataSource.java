package com.swp391.scientific_journal_tracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ApiDataSources")
public class ApiDataSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApiDataSourceId", nullable = false, unique = true)
    private Long apiDataSourceId;
    @Column(name = "Name", nullable = false, columnDefinition = "VARCHAR(100) COLLATE utf8mb4_unicode_ci")
    private String name;
    @Column(name = "BaseURL", nullable = false, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    private String baseUrl;
    @Column(name = "LastSyncTime", nullable = true)
    private LocalDateTime lastSyncTime;
}
