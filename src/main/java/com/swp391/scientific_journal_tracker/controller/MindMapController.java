package com.swp391.scientific_journal_tracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;

import com.swp391.scientific_journal_tracker.dto.response.MindMapResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.service.MindMapService;

import lombok.RequiredArgsConstructor;

/** API tạo mind map động từ Keyword hoặc Topic đã có trong catalog. */
@RestController
@RequestMapping("/api/mind-map")
@RequiredArgsConstructor
public class MindMapController {

    private final MindMapService mindMapService;

    /**
     * Research Lab là chức năng phân tích chuyên sâu, chỉ dành cho Researcher
     * và Admin. Lecturer sử dụng dashboard, trend và report cơ bản thay thế.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public MindMapResponse getMindMap(
            @RequestParam String type,
            @RequestParam Long id,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear) {
        return mindMapService.getMindMap(type, id, limit, fromYear, toYear);
    }

    /**
     * Lay cac paper dong xuat hien lam bang chung cho mot canh Mind Map.
     * Frontend goi API nay khi nguoi dung click vao edge hoac node con.
     */
    @GetMapping("/evidence")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public Page<PaperResponse> getEvidence(
            @RequestParam String rootType,
            @RequestParam Long rootId,
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return mindMapService.getEvidencePapers(
                rootType, rootId, targetType, targetId, page, size);
    }
}
