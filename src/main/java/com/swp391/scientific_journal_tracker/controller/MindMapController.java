package com.swp391.scientific_journal_tracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import com.swp391.scientific_journal_tracker.dto.response.MindMapResponse;
import com.swp391.scientific_journal_tracker.security.ResearchAccessLevel;
import com.swp391.scientific_journal_tracker.security.ResearchAccessPolicy;
import com.swp391.scientific_journal_tracker.service.MindMapService;

import lombok.RequiredArgsConstructor;

/** API tạo mind map động từ Keyword hoặc Topic đã có trong catalog. */
@RestController
@RequestMapping("/api/mind-map")
@RequiredArgsConstructor
public class MindMapController {

    private final MindMapService mindMapService;
    private final ResearchAccessPolicy researchAccessPolicy;

    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'RESEARCHER', 'ADMIN')")
    public MindMapResponse getMindMap(
            @RequestParam String type,
            @RequestParam Long id,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        ResearchAccessLevel accessLevel = researchAccessPolicy.resolve(authentication);
        if (accessLevel == ResearchAccessLevel.BASIC) {
            return mindMapService.getBasicMindMap(type, id);
        }

        return mindMapService.getMindMap(type, id, limit);
    }
}
