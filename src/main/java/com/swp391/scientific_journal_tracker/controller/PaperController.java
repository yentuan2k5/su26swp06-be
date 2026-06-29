package com.swp391.scientific_journal_tracker.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.service.PaperService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    /**
     * GET /api/papers
     *
     * Query params:
     * search - tìm chung trong title / abstract / authors / keyword / journal /
     * topic
     * author - lọc theo tác giả
     * keyword - lọc theo keyword
     * journal - lọc theo journal
     * topic - lọc theo topic
     * year - lọc đúng một năm
     * yearFrom - lọc từ năm
     * yearTo - lọc đến năm
     * page - số trang, mặc định 0
     * size - số kết quả, mặc định 10, tối đa 50
     * sortBy - year / citationCount / title / researchPaperId
     * sortDir - asc / desc
     */
    @GetMapping
    public Page<PaperResponse> getPapers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String journal,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return paperService.getPapers(
                search,
                author,
                keyword,
                journal,
                topic,
                year,
                yearFrom,
                yearTo,
                page,
                size,
                sortBy,
                sortDir);
    }

    @GetMapping("/{id}")
    public PaperResponse getPaperById(@PathVariable Long id) {
        return paperService.getPaperById(id);
    }
}