package com.swp391.scientific_journal_tracker.service;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import com.swp391.scientific_journal_tracker.dto.response.KeywordResponse;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KeywordService {
    private static final int MAX_SUGGESTION_PAGE_SIZE = 20;

    private final KeywordRepository keywordRepository;

    public List<KeywordResponse> getAllKeywords() {
        return keywordRepository.findAll()
                .stream()
                .map(KeywordResponse::fromEntity)
                .toList();
    }

    /**
     * Tìm keyword dạng autocomplete để không phải tải toàn bộ catalog về frontend.
     *
     * @param query chuỗi người dùng đang nhập
     * @param page  trang kết quả, bắt đầu từ 0
     * @param size  số kết quả mỗi trang, tối đa 20
     * @return các keyword có Term chứa query, sắp xếp theo tên
     */
    public List<KeywordResponse> getKeywordSuggestions(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_SUGGESTION_PAGE_SIZE));

        return keywordRepository
                .findByTermContainingIgnoreCaseOrderByTermAsc(
                        query.trim(),
                        PageRequest.of(safePage, safeSize))
                .stream()
                .map(KeywordResponse::fromEntity)
                .toList();
    }
}
