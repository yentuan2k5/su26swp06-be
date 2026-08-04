package com.swp391.scientific_journal_tracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.MindMapResponse;
import com.swp391.scientific_journal_tracker.service.MindMapService;

import lombok.RequiredArgsConstructor;

/** API tạo mind map động từ Keyword hoặc Topic đã có trong catalog. */
@RestController
@RequestMapping("/api/mind-map")
@RequiredArgsConstructor
public class MindMapController {

    private final MindMapService mindMapService;

    @GetMapping
    public MindMapResponse getMindMap(
            @RequestParam String type,
            @RequestParam Long id,
            @RequestParam(defaultValue = "5") int limit) {
        return mindMapService.getMindMap(type, id, limit);
    }
}
