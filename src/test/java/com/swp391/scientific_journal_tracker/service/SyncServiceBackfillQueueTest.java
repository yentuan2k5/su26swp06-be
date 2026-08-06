package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.entity.SyncLog;
import com.swp391.scientific_journal_tracker.repository.ApiDataSourceRepository;
import com.swp391.scientific_journal_tracker.repository.AuthorRepository;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;

@ExtendWith(MockitoExtension.class)
class SyncServiceBackfillQueueTest {

    @Mock
    private OpenAlexClient openAlexClient;

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private ResearchPaperRepository paperRepository;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private ResearchTopicRepository researchTopicRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private ApiDataSourceRepository apiDataSourceRepository;

    @InjectMocks
    private SyncService syncService;

    @Test
    void queuesBackfillAndPreventsAnotherSyncOperation() {
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> {
            SyncLog syncLog = invocation.getArgument(0);
            syncLog.setSyncLogId(12L);
            return syncLog;
        });

        SyncLogResponse response = syncService.queueBackfillFromOpenAlex(
                2024,
                2024,
                List.of("17"),
                7000);

        assertEquals(12L, response.getSyncLogId());
        assertEquals(SyncLog.Status.RUNNING, response.getStatus());
        assertThrows(IllegalStateException.class, () -> syncService.queueBackfillFromOpenAlex(
                2024,
                2024,
                List.of("17"),
                7000));
    }
}
