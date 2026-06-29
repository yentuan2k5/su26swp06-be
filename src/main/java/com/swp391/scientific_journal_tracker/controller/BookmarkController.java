package com.swp391.scientific_journal_tracker.controller;


import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.entity.Bookmark;
import com.swp391.scientific_journal_tracker.service.BookmarkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
        private final BookmarkService bookmarkService;

        @PostMapping
        public Bookmark saveBookmark(
                @RequestParam Long userId,
                @RequestParam Long paperId) {

                return bookmarkService.saveBookmark(
                        userId,
                        paperId);
        }

        @DeleteMapping
        public String removeBookmark(
                @RequestParam Long userId,
                @RequestParam Long paperId) {

                bookmarkService.removeBookmark(
                        userId,
                        paperId);

                return "Bookmark removed successfully";
        }

        @GetMapping("/{userId}")
        public List<Bookmark> getBookmarks(
                @PathVariable Long userId) {

                return bookmarkService.getBookmarksByUser(
                        userId);
        }
}
