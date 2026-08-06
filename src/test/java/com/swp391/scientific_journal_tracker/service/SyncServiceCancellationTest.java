package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.entity.ApiDataSource;
import com.swp391.scientific_journal_tracker.entity.SyncLog;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import com.swp391.scientific_journal_tracker.repository.ApiDataSourceRepository;
import com.swp391.scientific_journal_tracker.repository.AuthorRepository;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncServiceCancellationTest {

    @Mock private OpenAlexClient openAlexClient;
    @Mock private SyncLogRepository syncLogRepository;
    @Mock private ResearchPaperRepository paperRepository;
    @Mock private JournalRepository journalRepository;
    @Mock private KeywordRepository keywordRepository;
    @Mock private ResearchTopicRepository researchTopicRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuthorRepository authorRepository;
    @Mock private ApiDataSourceRepository apiDataSourceRepository;

    @InjectMocks private SyncService syncService;

    @Test
    void cancelSyncClosesOrphanedRunningLog() {
        SyncLog runningLog = syncLog(42L, Status.RUNNING);
        when(syncLogRepository.findById(42L)).thenReturn(Optional.of(runningLog));
        when(syncLogRepository.save(any(SyncLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SyncLogResponse response = syncService.cancelSync(42L);

        assertEquals(Status.CANCELLED, response.getStatus());
        assertEquals(0, response.getPaperSynced());
        assertNotNull(response.getFinishedAt());
        verify(syncLogRepository).save(runningLog);
    }

    @Test
    void cancelSyncSignalsActiveBackfillWorker() throws Exception {
        AtomicReference<SyncLog> savedLog = new AtomicReference<>();
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        ApiDataSource source = new ApiDataSource();
        source.setApiDataSourceId(1L);
        source.setName("OpenAlex");

        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> {
            SyncLog log = invocation.getArgument(0);
            if (log.getSyncLogId() == null) log.setSyncLogId(88L);
            savedLog.set(log);
            return log;
        });
        when(syncLogRepository.findById(88L))
                .thenAnswer(invocation -> Optional.ofNullable(savedLog.get()));
        when(apiDataSourceRepository.findByNameIgnoreCase("OpenAlex"))
                .thenReturn(Optional.of(source));
        when(openAlexClient.fetchWorksByFilterInPages(anyString(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    fetchStarted.countDown();
                    releaseFetch.await(3, TimeUnit.SECONDS);
                    return 0;
                });

        CompletableFuture<SyncLogResponse> runningJob = CompletableFuture.supplyAsync(
                () -> syncService.backfillFromOpenAlex(2020, 2020, List.of("17"), 1));

        if (!fetchStarted.await(3, TimeUnit.SECONDS)) {
            throw new AssertionError("Backfill did not reach the OpenAlex fetch step.");
        }

        SyncLogResponse cancellationResponse = syncService.cancelSync(88L);
        releaseFetch.countDown();
        SyncLogResponse workerResponse = runningJob.get(3, TimeUnit.SECONDS);

        assertEquals(Status.CANCELLED, cancellationResponse.getStatus());
        assertEquals(Status.CANCELLED, workerResponse.getStatus());
    }

    @Test
    void cancelSyncIsIdempotentForFinishedLog() {
        SyncLog finishedLog = syncLog(7L, Status.SUCCESS);
        finishedLog.setFinishedAt(LocalDateTime.now());
        when(syncLogRepository.findById(7L)).thenReturn(Optional.of(finishedLog));

        SyncLogResponse response = syncService.cancelSync(7L);

        assertEquals(Status.SUCCESS, response.getStatus());
        verify(syncLogRepository, never()).save(any());
    }

    @Test
    void cancelSyncRejectsUnknownLog() {
        when(syncLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> syncService.cancelSync(999L));
    }

    private SyncLog syncLog(long id, Status status) {
        SyncLog log = new SyncLog();
        log.setSyncLogId(id);
        log.setSourceApi("openalex-backfill");
        log.setStatus(status);
        log.setPaperSynced(0);
        log.setStartedAt(LocalDateTime.now().minusMinutes(5));
        return log;
    }
}
