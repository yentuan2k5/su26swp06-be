package com.swp391.scientific_journal_tracker.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MindMapNodeResponse {
    private String id;
    private String type;
    private String label;
    private long paperCount;
    private long recentPaperCount;
    private long previousPaperCount;
    private String trendStatus;
    private int depth;
    /** Tong so paper cua thuc the trong toan bo catalog. */
    private long catalogPaperCount;
    /** So paper chung voi node goc; node goc co gia tri bang catalogPaperCount. */
    private long sharedPaperCount;

    public MindMapNodeResponse(
            String id,
            String type,
            String label,
            long paperCount,
            long recentPaperCount,
            long previousPaperCount,
            String trendStatus,
            int depth) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.paperCount = paperCount;
        this.recentPaperCount = recentPaperCount;
        this.previousPaperCount = previousPaperCount;
        this.trendStatus = trendStatus;
        this.depth = depth;
        this.catalogPaperCount = paperCount;
        this.sharedPaperCount = paperCount;
    }

    public MindMapNodeResponse(
            String id,
            String type,
            String label,
            long paperCount,
            long recentPaperCount,
            long previousPaperCount,
            String trendStatus,
            int depth,
            long catalogPaperCount,
            long sharedPaperCount) {
        this(id, type, label, paperCount, recentPaperCount, previousPaperCount, trendStatus, depth);
        this.catalogPaperCount = catalogPaperCount;
        this.sharedPaperCount = sharedPaperCount;
    }
}
