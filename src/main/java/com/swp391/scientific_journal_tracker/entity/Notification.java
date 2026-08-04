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
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Table(name = "Notifications", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_notification_user_paper_type",
                columnNames = { "UserID", "ResearchPaperId", "Type" })
})
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor

public class Notification {
    public static final String TYPE_NEW_PAPER = "NEW_PAPER";

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "NotificationId", nullable = false, unique = true)
    private Long notificationId;
    @Column(name = "UserID", nullable = false)
    private Long userId;
    @Column(name = "Message", nullable = false)
    private String message;
    /** ID bài báo tạo ra thông báo, dùng để frontend mở đúng chi tiết paper. */
    @Column(name = "ResearchPaperId")
    private Long paperId;
    /** Lý do nhận thông báo, ví dụ đang follow journal hoặc topic nào. */
    @Column(name = "MatchedReason", length = 255)
    private String matchedReason;
    /** Loại thông báo, hiện tại hỗ trợ NEW_PAPER. */
    @Column(name = "Type", length = 50)
    private String type = TYPE_NEW_PAPER;
    @Column(name = "IsRead", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isRead = false;
    @Column(name = "SendAt", nullable = false, insertable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime sendAt = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private User user;
}
