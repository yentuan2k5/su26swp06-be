package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.scientific_journal_tracker.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdOrderBySendAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderBySendAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadTrueOrderBySendAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByNotificationIdAndUserId(Long notificationId, Long userId);

    List<Notification> findByPaperIdAndTypeAndUserIdIn(
            Long paperId,
            String type,
            Collection<Long> userIds);
}
