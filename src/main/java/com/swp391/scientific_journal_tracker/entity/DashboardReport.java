package com.swp391.scientific_journal_tracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DashboardReports")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DashboardReport {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "DashboardReportId", nullable = false, unique = true)
    private Long dashboardReportId;
    @Column(name = "Count", nullable = false, columnDefinition = "VarChar(255) COLLATE utf8mb4_unicode_ci")
    private String title;
    @Column(name = "Content", columnDefinition = "Text COLLATE utf8mb4_unicode_ci")
    private String content;
    @Column(name = "GeneratedAt", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime generatedAt = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "UserId", nullable = false)
    private User user;
}
