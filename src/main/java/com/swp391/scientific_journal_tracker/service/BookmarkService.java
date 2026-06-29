package com.swp391.scientific_journal_tracker.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.BookmarkResponse;
import com.swp391.scientific_journal_tracker.entity.Bookmark;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.DuplicateResourceException;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.BookmarkRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookmarkService {

        private final BookmarkRepository bookmarkRepository;
        private final UserRepository userRepository;
        private final ResearchPaperRepository researchPaperRepository;

        @Transactional
        public BookmarkResponse saveBookmark(Long paperId, Authentication authentication) {
                User user = getCurrentUser(authentication);

                if (bookmarkRepository.existsByUserUserIdAndResearchPaperResearchPaperId(
                                user.getUserId(),
                                paperId)) {
                        throw new DuplicateResourceException("Paper already bookmarked");
                }

                ResearchPaper paper = researchPaperRepository.findById(paperId)
                                .orElseThrow(() -> new ResourceNotFoundException("Paper not found"));

                Bookmark bookmark = new Bookmark();
                bookmark.setUser(user);
                bookmark.setResearchPaper(paper);

                Bookmark savedBookmark = bookmarkRepository.save(bookmark);

                return BookmarkResponse.fromEntity(savedBookmark);
        }

        @Transactional
        public void removeBookmark(Long paperId, Authentication authentication) {
                User user = getCurrentUser(authentication);

                Bookmark bookmark = bookmarkRepository
                                .findByUserUserIdAndResearchPaperResearchPaperId(
                                                user.getUserId(),
                                                paperId)
                                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found"));

                bookmarkRepository.delete(bookmark);
        }

        @Transactional(readOnly = true)
        public List<BookmarkResponse> getMyBookmarks(Authentication authentication) {
                User user = getCurrentUser(authentication);

                return bookmarkRepository
                                .findByUserUserIdOrderBySavedAtDesc(user.getUserId())
                                .stream()
                                .map(BookmarkResponse::fromEntity)
                                .toList();
        }

        @Transactional(readOnly = true)
        public boolean isBookmarked(Long paperId, Authentication authentication) {
                User user = getCurrentUser(authentication);

                return bookmarkRepository.existsByUserUserIdAndResearchPaperResearchPaperId(
                                user.getUserId(),
                                paperId);
        }

        private User getCurrentUser(Authentication authentication) {
                if (authentication == null || authentication.getName() == null) {
                        throw new ResourceNotFoundException("User not authenticated");
                }

                String username = authentication.getName();

                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
}