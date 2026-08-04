package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MindMapResponse {
    private MindMapNodeResponse root;
    private List<MindMapNodeResponse> nodes;
    private List<MindMapEdgeResponse> edges;
}
