package com.swp391.scientific_journal_tracker.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.NotificationResponse;
import com.swp391.scientific_journal_tracker.entity.Journal;
import com.swp391.scientific_journal_tracker.entity.Notification;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.NotificationRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JournalRepository journalRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return notificationRepository
                .findByUserIdOrderBySendAtDesc(user.getUserId())
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnreadNotifications(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return notificationRepository
                .findByUserIdAndIsReadFalseOrderBySendAtDesc(user.getUserId())
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countMyUnreadNotifications(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return notificationRepository.countByUserIdAndIsReadFalse(user.getUserId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);

        Notification savedNotification = notificationRepository.save(notification);

        return NotificationResponse.fromEntity(savedNotification);
    }

    @Transactional
    public void markAllAsRead(Authentication authentication) {
        User user = getCurrentUser(authentication);

        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderBySendAtDesc(user.getUserId());

        unreadNotifications.forEach(notification -> notification.setRead(true));

        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void createNewPaperNotifications(ResearchPaper paper) {
        if (paper == null || paper.getResearchPaperId() == null) {
            return;
        }

        Map<Long, User> recipients = new LinkedHashMap<>();

        if (paper.getJournalId() != null) {
            journalRepository.findById(paper.getJournalId())
                    .ifPresent(journal -> addJournalFollowers(recipients, journal));
        }

        if (paper.getResearchTopics() != null) {
            for (ResearchTopic topic : paper.getResearchTopics()) {
                addTopicFollowers(recipients, topic);
            }
        }

        if (recipients.isEmpty()) {
            return;
        }

        String title = paper.getTitle() == null ? "Untitled paper" : paper.getTitle();

        String message = "New paper published: " + title;

        List<Notification> notifications = recipients.values()
                .stream()
                .map(user -> {
                    Notification notification = new Notification();
                    notification.setUserId(user.getUserId());
                    notification.setMessage(message);
                    notification.setRead(false);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);
    }

    private void addJournalFollowers(Map<Long, User> recipients, Journal journal) {
        if (journal == null || journal.getFollowers() == null) {
            return;
        }

        for (User user : journal.getFollowers()) {
            if (user != null && user.getUserId() != null) {
                recipients.putIfAbsent(user.getUserId(), user);
            }
        }
    }

    private void addTopicFollowers(Map<Long, User> recipients, ResearchTopic topic) {
        if (topic == null || topic.getFollowers() == null) {
            return;
        }

        for (User user : topic.getFollowers()) {
            if (user != null && user.getUserId() != null) {
                recipients.putIfAbsent(user.getUserId(), user);
            }
        }
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}