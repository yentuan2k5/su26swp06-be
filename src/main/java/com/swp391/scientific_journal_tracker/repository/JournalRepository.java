package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.swp391.scientific_journal_tracker.entity.Journal;

public interface JournalRepository extends JpaRepository<Journal, Long> {
    Optional<Journal> findByTitle(String title);

    Optional<Journal> findByIssn(String issn);

    boolean existsByIssn(String issn);

    List<Journal> findByTitleContainingIgnoreCase(String keyword);

    List<Journal> findByPublisherContainingIgnoreCase(String publisher);

    List<Journal> findByFieldContainingIgnoreCase(String field);

    boolean existsByTitle(String title);

    @Query("""
            SELECT DISTINCT j
            FROM Journal j
            WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(j.issn, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(j.publisher, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(j.field, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY j.title ASC
            """)
    List<Journal> searchJournals(@Param("keyword") String keyword);

    @Query("""
            SELECT
                j.journalId,
                j.title,
                j.issn,
                j.publisher,
                j.field,
                COUNT(DISTINCT p),
                COUNT(DISTINCT f)
            FROM Journal j
            LEFT JOIN j.researchPapers p
            LEFT JOIN j.followers f
            GROUP BY j.journalId, j.title, j.issn, j.publisher, j.field
            ORDER BY COUNT(DISTINCT p) DESC
            """)
    List<Object[]> findTopJournals(Pageable pageable);
}
