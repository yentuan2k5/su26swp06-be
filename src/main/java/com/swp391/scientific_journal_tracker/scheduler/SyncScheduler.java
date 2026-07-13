package com.swp391.scientific_journal_tracker.scheduler;

import com.swp391.scientific_journal_tracker.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SyncService syncService;

    /**
     * Chạy mỗi ngày lúc 2:00 AM
     * Cron format: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 40 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduledSync() {
        log.info("Bắt đầu đồng bộ bài báo lúc 02:38 sáng...");
        syncService.syncFromOpenAlex();
    }

    /**
     * Hoặc dùng fixedDelay để test: chạy mỗi 30 phút
     * Uncomment dòng dưới, comment @Scheduled cron ở trên
     */
    // @Scheduled(fixedDelay = 30 * 60 * 1000)
    // public void scheduledSyncEvery30Min() { ... }
}
