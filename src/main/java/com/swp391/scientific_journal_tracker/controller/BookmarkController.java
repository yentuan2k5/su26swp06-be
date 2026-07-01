package com.swp391.scientific_journal_tracker.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.BookmarkResponse;
import com.swp391.scientific_journal_tracker.service.BookmarkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

        private final BookmarkService bookmarkService;

        @PostMapping("/{paperId}")
        public BookmarkResponse saveBookmark(
                        @PathVariable Long paperId,
                        Authentication authentication) {
                return bookmarkService.saveBookmark(paperId, authentication);
        }

        @DeleteMapping("/{paperId}")
        public Map<String, String> removeBookmark(
                        @PathVariable Long paperId,
                        Authentication authentication) {
                bookmarkService.removeBookmark(paperId, authentication);

                return Map.of("message", "Bookmark removed successfully");
        }

        @GetMapping
        public List<BookmarkResponse> getMyBookmarks(Authentication authentication) {
                return bookmarkService.getMyBookmarks(authentication);
        }

        @GetMapping("/check/{paperId}")
        public Map<String, Boolean> checkBookmarked(
                        @PathVariable Long paperId,
                        Authentication authentication) {
                boolean bookmarked = bookmarkService.isBookmarked(paperId, authentication);

                return Map.of("bookmarked", bookmarked);
        }

        @PostMapping("/keywords/{keywordId}")
        public BookmarkResponse saveKeywordBookmark(
                        @PathVariable Long keywordId,
                        Authentication authentication) {
                return bookmarkService.saveKeywordBookmark(keywordId, authentication);
        }

        @DeleteMapping("/keywords/{keywordId}")
        public Map<String, String> removeKeywordBookmark(
                        @PathVariable Long keywordId,
                        Authentication authentication) {
                bookmarkService.removeKeywordBookmark(keywordId, authentication);

                return Map.of("message", "Keyword bookmark removed successfully");
        }

        @GetMapping("/keywords")
        public List<BookmarkResponse> getMyKeywordBookmarks(Authentication authentication) {
                return bookmarkService.getMyKeywordBookmarks(authentication);
        }

        @GetMapping("/keywords/check/{keywordId}")
        public Map<String, Boolean> checkKeywordBookmarked(
                        @PathVariable Long keywordId,
                        Authentication authentication) {
                boolean bookmarked = bookmarkService.isKeywordBookmarked(keywordId, authentication);

                return Map.of("bookmarked", bookmarked);
        }
}