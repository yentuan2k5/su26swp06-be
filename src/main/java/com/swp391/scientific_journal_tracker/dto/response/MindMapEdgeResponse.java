package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MindMapEdgeResponse {
    private String sourceId;
    private String targetId;
    private String relation;
}
