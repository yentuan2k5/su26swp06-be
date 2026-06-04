package com.swp391.scientific_journal_tracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.*;

@Entity
@Table(name = "Notifications")
@AllArgsConstructor
@Data
@NoArgsConstructor

public class Notification {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "NotificationId", nullable = false, unique = true)
    private Long notificationId;
    @Column(name = "UserID", nullable = false)
    private Long userId;
    @Column(name = "Message", nullable = false)
    private String message;
    @Column(name = "IsRead", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isRead = false;
    @Column(name = "SendAt", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime sendAt = LocalDateTime.now();
}
