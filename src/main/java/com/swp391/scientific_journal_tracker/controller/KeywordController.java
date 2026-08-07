package com.swp391.scientific_journal_tracker.controller;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.swp391.scientific_journal_tracker.dto.response.KeywordResponse;
import com.swp391.scientific_journal_tracker.service.KeywordService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordController {
    private final KeywordService keywordService;

    @GetMapping
    public List<KeywordResponse> getAllKeywords() {
        return keywordService.getAllKeywords();
    }

    /**
     * API autocomplete keyword. Dùng cho ô tìm kiếm trend/compare để tránh tải
     * toàn bộ keyword của catalog xuống trình duyệt.
     */
    @GetMapping("/suggestions")
    public List<KeywordResponse> getKeywordSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return keywordService.getKeywordSuggestions(q, page, size);
    }
}
