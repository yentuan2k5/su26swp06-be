package com.swp391.scientific_journal_tracker.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.swp391.scientific_journal_tracker.dto.response.JournalResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.entity.Journal;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.DuplicateResourceException;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final ResearchPaperRepository researchPaperRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public JournalResponse followJournal(Long journalId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Journal journal = getJournalById(journalId);

        boolean alreadyFollowed = user.getFollowingJournals()
                .stream()
                .anyMatch(j -> j.getJournalId().equals(journalId));

        if (alreadyFollowed) {
            throw new DuplicateResourceException("Journal already followed");
        }

        user.getFollowingJournals().add(journal);

        boolean followerExists = journal.getFollowers()
                .stream()
                .anyMatch(u -> u.getUserId().equals(user.getUserId()));

        if (!followerExists) {
            journal.getFollowers().add(user);
        }

        userRepository.save(user);

        return JournalResponse.fromEntity(journal);
    }

    @Transactional
    public JournalResponse unfollowJournal(Long journalId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Journal journal = getJournalById(journalId);

        boolean removed = user.getFollowingJournals()
                .removeIf(j -> j.getJournalId().equals(journalId));

        if (!removed) {
            throw new ResourceNotFoundException("Journal follow not found");
        }

        journal.getFollowers()
                .removeIf(u -> u.getUserId().equals(user.getUserId()));

        userRepository.save(user);

        return JournalResponse.fromEntity(journal);
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> getMyFollowingJournals(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return user.getFollowingJournals()
                .stream()
                .map(JournalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isJournalFollowed(Long journalId, Authentication authentication) {
        User user = getCurrentUser(authentication);

        return user.getFollowingJournals()
                .stream()
                .anyMatch(j -> j.getJournalId().equals(journalId));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Journal getJournalById(Long journalId) {
        return journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found with id: " + journalId));
    }
}