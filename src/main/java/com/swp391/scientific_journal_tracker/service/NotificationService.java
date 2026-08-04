package com.swp391.scientific_journal_tracker.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JournalRepository journalRepository;
    private final ResearchPaperRepository paperRepository;

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
    public void createNewPaperNotifications(
            Long researchPaperId) {
        ResearchPaper paper = paperRepository
                .findById(researchPaperId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy paper để tạo notification: "
                                + researchPaperId));

        /*
         * Giữ nguyên toàn bộ logic tạo notification hiện tại phía dưới.
         * Từ đây dùng biến paper vừa query lại từ database.
         */

        Map<Long, String> recipientReasons = new LinkedHashMap<>();

        if (paper.getJournalId() != null) {
            journalRepository.findById(paper.getJournalId())
                    .ifPresent(journal -> addJournalFollowers(recipientReasons, journal));
        }

        if (paper.getResearchTopics() != null) {
            for (ResearchTopic topic : paper.getResearchTopics()) {
                addTopicFollowers(recipientReasons, topic);
            }
        }

        if (recipientReasons.isEmpty()) {
            return;
        }

        String title = paper.getTitle() == null ? "Untitled paper" : paper.getTitle();

        String message = "New paper published: " + title;

        Set<Long> recipientIds = recipientReasons.keySet();
        Set<Long> alreadyNotified = new HashSet<>(notificationRepository
                .findByPaperIdAndTypeAndUserIdIn(
                        paper.getResearchPaperId(),
                        Notification.TYPE_NEW_PAPER,
                        recipientIds)
                .stream()
                .map(Notification::getUserId)
                .toList());

        List<Notification> notifications = recipientReasons.entrySet()
                .stream()
                .filter(entry -> !alreadyNotified.contains(entry.getKey()))
                .map(entry -> {
                    Notification notification = new Notification();
                    notification.setUserId(entry.getKey());
                    notification.setMessage(message);
                    notification.setPaperId(paper.getResearchPaperId());
                    notification.setMatchedReason(entry.getValue());
                    notification.setType(Notification.TYPE_NEW_PAPER);
                    notification.setRead(false);
                    return notification;
                })
                .toList();

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    private void addJournalFollowers(Map<Long, String> recipientReasons, Journal journal) {
        if (journal == null || journal.getFollowers() == null) {
            return;
        }

        for (User user : journal.getFollowers()) {
            if (user != null && user.getUserId() != null) {
                addReason(
                        recipientReasons,
                        user.getUserId(),
                        "Following journal: " + journal.getTitle());
            }
        }
    }

    private void addTopicFollowers(Map<Long, String> recipientReasons, ResearchTopic topic) {
        if (topic == null || topic.getFollowers() == null) {
            return;
        }

        for (User user : topic.getFollowers()) {
            if (user != null && user.getUserId() != null) {
                addReason(
                        recipientReasons,
                        user.getUserId(),
                        "Following topic: " + topic.getName());
            }
        }
    }

    private void addReason(
            Map<Long, String> recipientReasons,
            Long userId,
            String reason) {
        recipientReasons.merge(
                userId,
                reason,
                (existing, incoming) -> existing.equals(incoming)
                        ? existing
                        : existing + "; " + incoming);
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
