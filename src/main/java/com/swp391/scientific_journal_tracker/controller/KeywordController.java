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
}
