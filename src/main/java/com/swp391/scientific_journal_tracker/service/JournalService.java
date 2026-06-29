package com.swp391.scientific_journal_tracker.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.JournalResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.entity.Journal;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final ResearchPaperRepository researchPaperRepository;

    @Transactional(readOnly = true)
    public List<JournalResponse> getAllJournals() {
        return journalRepository.findAll(Sort.by("title").ascending())
                .stream()
                .map(JournalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> searchJournals(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllJournals();
        }

        return journalRepository.searchJournals(keyword.trim())
                .stream()
                .map(JournalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> getTopJournals(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        return journalRepository.findTopJournals(PageRequest.of(0, safeLimit))
                .stream()
                .map(row -> new JournalResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        ((Number) row[5]).longValue(),
                        ((Number) row[6]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public JournalResponse getJournalDetail(Long journalId) {
        Journal journal = getJournalById(journalId);
        return JournalResponse.fromEntity(journal);
    }

    @Transactional(readOnly = true)
    public Page<PaperResponse> getPapersByJournal(Long journalId, int page, int size) {
        getJournalById(journalId);

        return researchPaperRepository
                .findByJournalJournalId(
                        journalId,
                        PageRequest.of(page, size, Sort.by("year").descending()))
                .map(PaperResponse::fromEntity);
    }

    private Journal getJournalById(Long journalId) {
        return journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + journalId));
    }
}