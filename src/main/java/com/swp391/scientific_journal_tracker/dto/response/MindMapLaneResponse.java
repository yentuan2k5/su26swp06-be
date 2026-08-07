package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mo ta muc do du lieu cua mot nhanh Mind Map.
 * Client dung thong tin nay de chi ve nhanh co bang chung, hoac hien ly do
 * khi catalog chua du metadata cho nhanh do.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MindMapLaneResponse {
    private String type;
    private String label;
    private int candidateCount;
    private int strongCandidateCount;
    private int limitedCandidateCount;
    private int displayedCount;
    private String evidenceLevel;
    private String message;
}
