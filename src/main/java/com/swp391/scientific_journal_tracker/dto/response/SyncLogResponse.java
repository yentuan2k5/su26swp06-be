package com.swp391.scientific_journal_tracker.dto.response;

import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class SyncLogResponse {
    private Long syncLogId;
    private String sourceApi;
    private Status status;
    private Integer paperSynced;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    // Factory method để convert từ entity
    public static SyncLogResponse from(com.swp391.scientific_journal_tracker.entity.SyncLog log) {
        return SyncLogResponse.builder()
                .syncLogId(log.getSyncLogId())
                .sourceApi(log.getSourceApi())
                .status(log.getStatus())
                .paperSynced(log.getPaperSynced())
                .errorMessage(log.getErrorMessage())
                .startedAt(log.getStartedAt())
                .finishedAt(log.getFinishedAt())
                .build();
    }

}
