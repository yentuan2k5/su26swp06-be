package com.swp391.scientific_journal_tracker.service;

import java.util.List;

import org.springframework.stereotype.Service;

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


        public Bookmark saveBookmark(Long userId, Long paperId) {

                if (bookmarkRepository
                        .existsByUserUserIdAndResearchPaperResearchPaperId(
                                userId,
                                paperId)) {

                throw new DuplicateResourceException(
                        "Paper already bookmarked");
                }

                User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

                ResearchPaper paper = researchPaperRepository.findById(paperId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Paper not found"));

                Bookmark bookmark = new Bookmark();
                bookmark.setUser(user);
                bookmark.setResearchPaper(paper);

                return bookmarkRepository.save(bookmark);
        }

        public void removeBookmark(Long userId, Long paperId) {

                Bookmark bookmark = bookmarkRepository
                        .findByUserUserIdAndResearchPaperResearchPaperId(
                                userId,
                                paperId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Bookmark not found"));

                bookmarkRepository.delete(bookmark);
        }

        public List<Bookmark> getBookmarksByUser(Long userId) {

                return bookmarkRepository.findByUserUserId(userId);
        }
}