package com.swp391.scientific_journal_tracker.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MindMapEdgeResponse {
    private String sourceId;
    private String targetId;
    private String relation;
    private long sharedPaperCount;
    private long recentSharedPaperCount;
    private long previousSharedPaperCount;
    private double growthRate;
    private String trendStatus;
    private double associationScore;
    private double rankScore;

    public MindMapEdgeResponse(String sourceId, String targetId, String relation) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.relation = relation;
    }

    public MindMapEdgeResponse(
            String sourceId,
            String targetId,
            String relation,
            long sharedPaperCount,
            long recentSharedPaperCount,
            long previousSharedPaperCount,
            double growthRate,
            String trendStatus,
            double associationScore,
            double rankScore) {
        this(sourceId, targetId, relation);
        this.sharedPaperCount = sharedPaperCount;
        this.recentSharedPaperCount = recentSharedPaperCount;
        this.previousSharedPaperCount = previousSharedPaperCount;
        this.growthRate = growthRate;
        this.trendStatus = trendStatus;
        this.associationScore = associationScore;
        this.rankScore = rankScore;
    }
}
