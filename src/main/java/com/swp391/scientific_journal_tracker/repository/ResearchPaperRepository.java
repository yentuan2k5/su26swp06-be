package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp391.scientific_journal_tracker.entity.ResearchPaper;

public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, Long> {

    List<ResearchPaper> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    Optional<ResearchPaper> findByDoi(String doi);

    boolean existsByDoi(String doi);

    List<ResearchPaper> findByTitleContainingIgnoreCase(String title);

    List<ResearchPaper> findByAuthorsContainingIgnoreCase(String author);

    List<ResearchPaper> findByYear(Integer year);

    List<ResearchPaper> findBySourceApi(String sourceApi);

    List<ResearchPaper> findByJournalJournalId(Long journalId);

    List<ResearchPaper> findByApiDataSourceApiDataSourceId(Long apiDataSourceId);

    List<ResearchPaper> findTop10ByOrderByCitationCountDesc();

    List<ResearchPaper> findTop10ByOrderByYearDesc();

    long countBySourceApi(String sourceApi);

    @Query("""
        SELECT p.year, COUNT(p)
        FROM ResearchPaper p
        WHERE p.year IS NOT NULL
        GROUP BY p.year
        ORDER BY p.year DESC
    """)
    List<Object[]> countPapersByYear();

    @Query("""
        SELECT k.term, COUNT(p)
        FROM ResearchPaper p
        JOIN p.keywords k
        GROUP BY k.term
        ORDER BY COUNT(p) DESC
    """)
    List<Object[]> countTopKeywords(Pageable pageable);

    @Query("""
        SELECT j.title, COUNT(p)
        FROM ResearchPaper p
        JOIN p.journal j
        GROUP BY j.title
        ORDER BY COUNT(p) DESC
    """)
    List<Object[]> countTopJournals(Pageable pageable);

    @Query("""
        SELECT DISTINCT p
        FROM ResearchPaper p
        LEFT JOIN p.keywords k
        WHERE (:search IS NULL
            OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.authors) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:year IS NULL OR p.year = :year)
        AND (:keyword IS NULL OR LOWER(k.term) = LOWER(:keyword))
    """)
    Page<ResearchPaper> searchPapers(
            @Param("search") String search,
            @Param("year") Integer year,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
