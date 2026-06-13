package com.swp391.scientific_journal_tracker.service;
import java.util.List;

import org.springframework.stereotype.Service;


import com.swp391.scientific_journal_tracker.dto.response.KeywordResponse;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KeywordService {
    private final KeywordRepository keywordRepository;

    public List<KeywordResponse> getAllKeywords() {
        return keywordRepository.findAll()
                .stream()
                .map(KeywordResponse::fromEntity)
                .toList();
    }
}
