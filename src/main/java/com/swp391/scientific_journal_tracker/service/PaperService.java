package com.swp391.scientific_journal_tracker.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaperService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "year",
            "citationCount",
            "title",
            "researchPaperId");

    private final ResearchPaperRepository researchPaperRepository;

    @Transactional(readOnly = true)
    public Page<PaperResponse> getPapers(
            String search,
            String author,
            String keyword,
            String journal,
            String topic,
            Integer year,
            Integer yearFrom,
            Integer yearTo,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));

        String safeSortBy = normalizeSortBy(sortBy);
        Sort.Direction direction = normalizeSortDirection(sortDir);

        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(direction, safeSortBy));

        return researchPaperRepository
                .searchPapersAdvanced(
                        emptyToNull(search),
                        emptyToNull(author),
                        emptyToNull(keyword),
                        emptyToNull(journal),
                        emptyToNull(topic),
                        year,
                        yearFrom,
                        yearTo,
                        pageRequest)
                .map(PaperResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PaperResponse getPaperById(Long id) {
        ResearchPaper paper = researchPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id: " + id));

        return PaperResponse.fromEntity(paper);
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "year";
        }

        String normalized = sortBy.trim();

        if (!ALLOWED_SORT_FIELDS.contains(normalized)) {
            return "year";
        }

        return normalized;
    }

    private Sort.Direction normalizeSortDirection(String sortDir) {
        if (sortDir == null || sortDir.trim().isEmpty()) {
            return Sort.Direction.DESC;
        }

        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }

        return Sort.Direction.DESC;
    }
}