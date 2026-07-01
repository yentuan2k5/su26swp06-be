package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.scientific_journal_tracker.entity.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

        List<Bookmark> findByUserUserIdOrderBySavedAtDesc(Long userId);

        List<Bookmark> findByUserUserIdAndBookmarkTypeOrderBySavedAtDesc(
                        Long userId,
                        String bookmarkType);

        boolean existsByUserUserIdAndResearchPaperResearchPaperId(
                        Long userId,
                        Long researchPaperId);

        Optional<Bookmark> findByUserUserIdAndResearchPaperResearchPaperId(
                        Long userId,
                        Long researchPaperId);

        void deleteByUserUserIdAndResearchPaperResearchPaperId(
                        Long userId,
                        Long researchPaperId);

        boolean existsByUserUserIdAndKeywordKeywordId(
                        Long userId,
                        Long keywordId);

        Optional<Bookmark> findByUserUserIdAndKeywordKeywordId(
                        Long userId,
                        Long keywordId);

        void deleteByUserUserIdAndKeywordKeywordId(
                        Long userId,
                        Long keywordId);
}