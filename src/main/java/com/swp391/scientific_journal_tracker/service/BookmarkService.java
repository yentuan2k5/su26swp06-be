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
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookmarkService {

        private final BookmarkRepository bookmarkRepository;
        private final UserRepository userRepository;
        private final ResearchPaperRepository researchPaperRepository;
        private final KeywordRepository keywordRepository;

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
                bookmark.setBookmarkType("PAPER");

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
                                .findByUserUserIdAndBookmarkTypeOrderBySavedAtDesc(
                                                user.getUserId(),
                                                "PAPER")
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

        @Transactional
        public BookmarkResponse saveKeywordBookmark(Long keywordId, Authentication authentication) {
                User user = getCurrentUser(authentication);

                if (bookmarkRepository.existsByUserUserIdAndKeywordKeywordId(
                                user.getUserId(),
                                keywordId)) {
                        throw new DuplicateResourceException("Keyword already bookmarked");
                }

                Keyword keyword = keywordRepository.findById(keywordId)
                                .orElseThrow(() -> new ResourceNotFoundException("Keyword not found"));

                Bookmark bookmark = new Bookmark();
                bookmark.setUser(user);
                bookmark.setKeyword(keyword);
                bookmark.setBookmarkType("KEYWORD");

                Bookmark savedBookmark = bookmarkRepository.save(bookmark);

                return BookmarkResponse.fromEntity(savedBookmark);
        }

        @Transactional
        public void removeKeywordBookmark(Long keywordId, Authentication authentication) {
                User user = getCurrentUser(authentication);

                Bookmark bookmark = bookmarkRepository
                                .findByUserUserIdAndKeywordKeywordId(
                                                user.getUserId(),
                                                keywordId)
                                .orElseThrow(() -> new ResourceNotFoundException("Keyword bookmark not found"));

                bookmarkRepository.delete(bookmark);
        }

        @Transactional(readOnly = true)
        public List<BookmarkResponse> getMyKeywordBookmarks(Authentication authentication) {
                User user = getCurrentUser(authentication);

                return bookmarkRepository
                                .findByUserUserIdAndBookmarkTypeOrderBySavedAtDesc(
                                                user.getUserId(),
                                                "KEYWORD")
                                .stream()
                                .map(BookmarkResponse::fromEntity)
                                .toList();
        }

        @Transactional(readOnly = true)
        public boolean isKeywordBookmarked(Long keywordId, Authentication authentication) {
                User user = getCurrentUser(authentication);

                return bookmarkRepository.existsByUserUserIdAndKeywordKeywordId(
                                user.getUserId(),
                                keywordId);
        }
}