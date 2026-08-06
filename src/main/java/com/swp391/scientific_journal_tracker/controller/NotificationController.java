package com.swp391.scientific_journal_tracker.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.swp391.scientific_journal_tracker.dto.response.NotificationResponse;
import com.swp391.scientific_journal_tracker.service.NotificationService;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        return notificationService.getMyNotifications(authentication);
    }

    @GetMapping("/unread")
    public List<NotificationResponse> getMyUnreadNotifications(Authentication authentication) {
        return notificationService.getMyUnreadNotifications(authentication);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> countMyUnreadNotifications(Authentication authentication) {
        long unreadCount = notificationService.countMyUnreadNotifications(authentication);

        return Map.of("unreadCount", unreadCount);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable @Positive(message = "notificationId phải là số nguyên dương") Long notificationId,
            Authentication authentication) {
        return notificationService.markAsRead(notificationId, authentication);
    }

    @PutMapping("/read-all")
    public Map<String, String> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication);

        return Map.of("message", "All notifications marked as read");
    }

    @DeleteMapping("/{notificationId}")
    public Map<String, String> deleteNotification(
            @PathVariable @Positive(message = "notificationId phải là số nguyên dương") Long notificationId,
            Authentication authentication) {
        notificationService.deleteNotification(notificationId, authentication);

        return Map.of("message", "Notification deleted successfully");
    }
}
