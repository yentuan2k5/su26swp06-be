package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Số liệu vận hành đồng bộ dữ liệu, chỉ dành cho Admin. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOperationsResponse {
    private long openAlexPaperCount;
    private long successfulSyncCount;
    private long failedSyncCount;
    private SyncLogResponse latestSyncLog;
}
