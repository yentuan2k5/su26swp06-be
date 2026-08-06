package com.swp391.scientific_journal_tracker.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.swp391.scientific_journal_tracker.entity.Notification;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.NotificationRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private ResearchPaperRepository paperRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void deletesNotificationUsingCurrentUserInRepositoryQuery() {
        User user = new User();
        user.setUserId(7L);
        user.setUsername("student");
        Notification notification = new Notification();
        notification.setNotificationId(18L);
        notification.setUserId(7L);

        when(userRepository.findByUsername("student")).thenReturn(Optional.of(user));
        when(notificationRepository.findByNotificationIdAndUserId(18L, 7L))
                .thenReturn(Optional.of(notification));

        notificationService.deleteNotification(18L,
                new UsernamePasswordAuthenticationToken("student", null));

        verify(notificationRepository).findByNotificationIdAndUserId(18L, 7L);
        verify(notificationRepository).delete(notification);
    }
}
