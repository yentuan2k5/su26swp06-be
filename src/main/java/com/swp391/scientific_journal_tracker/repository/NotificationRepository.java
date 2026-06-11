package com.swp391.scientific_journal_tracker.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    List<Notification> findByUserIdAndIsReadTrue(Long userId);

    List<Notification> findByUserIdOrderBySendAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);   

}
