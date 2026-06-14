package com.swp391.scientific_journal_tracker.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaperService {
    private final ResearchPaperRepository researchPaperRepository;

    public Page<PaperResponse> getPapers(
            String search,
            String author,
            String keyword,
            String journal,
            String topic,
            Integer yearFrom,
            Integer yearTo,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("year").descending());

        return researchPaperRepository
                .searchPapers(
                        emptyToNull(search),
                        emptyToNull(author),
                        emptyToNull(keyword),
                        emptyToNull(journal),
                        emptyToNull(topic),
                        yearFrom,
                        yearTo,
                        pageable)
                .map(PaperResponse::fromEntity);
    }

    public PaperResponse getPaperById(Long id) {
        ResearchPaper paper = researchPaperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paper not found with id: " + id));

        return PaperResponse.fromEntity(paper);
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
