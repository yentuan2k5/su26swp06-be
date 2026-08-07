package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MindMapResponse {
    private MindMapNodeResponse root;
    private List<MindMapNodeResponse> nodes;
    private List<MindMapEdgeResponse> edges;
    private Integer fromYear;
    private Integer toYear;
    private Integer previousFromYear;
    private Integer previousToYear;

    /** Constructor cu giu tuong thich cho client dang dung API Mind Map. */
    public MindMapResponse(
            MindMapNodeResponse root,
            List<MindMapNodeResponse> nodes,
            List<MindMapEdgeResponse> edges) {
        this.root = root;
        this.nodes = nodes;
        this.edges = edges;
    }

    public MindMapResponse(
            MindMapNodeResponse root,
            List<MindMapNodeResponse> nodes,
            List<MindMapEdgeResponse> edges,
            Integer fromYear,
            Integer toYear,
            Integer previousFromYear,
            Integer previousToYear) {
        this(root, nodes, edges);
        this.fromYear = fromYear;
        this.toYear = toYear;
        this.previousFromYear = previousFromYear;
        this.previousToYear = previousToYear;
    }
}
