package com.swp391.scientific_journal_tracker.entity;

import java.time.LocalDateTime;

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
@Table(name = "SyncLogs")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SyncLog {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "SyncLogId", nullable = false, unique = true)
    private Long syncLogId;
    @Column(name = "SourceApi", columnDefinition = "Varchar(50) COLLATE utf8mb4_unicode_ci")
    private String sourceApi;
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, updatable = false, insertable = true)
    private Status status = Status.RUNNING;

    public enum Status {
        RUNNING,
        SUCCESS,
        FAILED
    }

    @Column(name = "PaperSynced", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer paperSynced = 0;
    @Column(name = "ErrorMessage", columnDefinition = "TEXT COLLATE utf8mb4_unicode_ci")
    private String errorMessage;
    @Column(name = "StartedAt", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime startedAt = LocalDateTime.now();
    @Column(name = "FinishedAt")
    private LocalDateTime finishedAt;
}
