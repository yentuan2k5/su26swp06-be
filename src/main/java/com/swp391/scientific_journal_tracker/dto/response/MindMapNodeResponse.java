package com.swp391.scientific_journal_tracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MindMapNodeResponse {
    private String id;
    private String type;
    private String label;
    private long paperCount;
    private long recentPaperCount;
    private long previousPaperCount;
    private String trendStatus;
    private int depth;
}
