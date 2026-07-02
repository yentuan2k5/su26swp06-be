package com.swp391.scientific_journal_tracker.dto.response;

import java.time.LocalDateTime;

import com.swp391.scientific_journal_tracker.entity.Notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {

    private Long notificationId;
    private Long userId;
    private String message;
    private boolean isRead;
    private LocalDateTime sendAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getUserId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getSendAt());
    }
}