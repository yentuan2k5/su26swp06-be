package com.swp391.scientific_journal_tracker.service;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAlexBackfillJobServiceTest {

    @Mock
    private SyncService syncService;

    @InjectMocks
    private OpenAlexBackfillJobService backfillJobService;

    @Test
    void delegatesQueuedBackfillToSyncService() {
        backfillJobService.runBackfill(18L, 2024, 2024, List.of("17"), 7000);

        verify(syncService).runQueuedBackfillFromOpenAlex(
                18L,
                2024,
                2024,
                List.of("17"),
                7000);
    }
}
