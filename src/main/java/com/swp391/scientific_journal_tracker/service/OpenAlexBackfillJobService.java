package com.swp391.scientific_journal_tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Chạy backfill OpenAlex ở background để HTTP request của Admin không phải chờ
 * toàn bộ quá trình gọi API và lưu dữ liệu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAlexBackfillJobService {

    private final SyncService syncService;

    @Async("openAlexBackfillTaskExecutor")
    public void runBackfill(
            Long syncLogId,
            int fromYear,
            int toYear,
            List<String> fieldIds,
            Integer maxResults) {
        log.info(
                "Bắt đầu job backfill nền. syncLogId={}, fromYear={}, toYear={}",
                syncLogId,
                fromYear,
                toYear);

        syncService.runQueuedBackfillFromOpenAlex(
                syncLogId,
                fromYear,
                toYear,
                fieldIds,
                maxResults);
    }
}
