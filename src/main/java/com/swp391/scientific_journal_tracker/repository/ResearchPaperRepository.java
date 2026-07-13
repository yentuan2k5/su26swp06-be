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

    List<ResearchPaper> findDistinctByAuthors_FullNameContainingIgnoreCase(
            String author);

    List<ResearchPaper> findByYear(Integer year);

    List<ResearchPaper> findBySourceApi(String sourceApi);

    List<ResearchPaper> findByJournalJournalId(Long journalId);

    Page<ResearchPaper> findByJournalJournalId(Long journalId, Pageable pageable);

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
            LEFT JOIN p.authors a
            WHERE (
                :search IS NULL
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:year IS NULL OR p.year = :year)
            AND (
                :keyword IS NULL
                OR LOWER(k.term) = LOWER(:keyword)
            )
            """)
    Page<ResearchPaper> searchPapers(
            @Param("search") String search,
            @Param("year") Integer year,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p
            FROM ResearchPaper p
            LEFT JOIN p.keywords k
            LEFT JOIN p.researchTopics t
            LEFT JOIN p.journal j
            LEFT JOIN p.authors a
            WHERE
                (
                    :search IS NULL
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(k.term) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
                )
                AND (
                    :author IS NULL
                    OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :author, '%'))
                )
                AND (
                    :keyword IS NULL
                    OR LOWER(k.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                AND (
                    :journal IS NULL
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :journal, '%'))
                )
                AND (
                    :topic IS NULL
                    OR LOWER(t.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(k.term) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
                AND (:year IS NULL OR p.year = :year)
                AND (:yearFrom IS NULL OR p.year >= :yearFrom)
                AND (:yearTo IS NULL OR p.year <= :yearTo)
            """, countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM ResearchPaper p
            LEFT JOIN p.keywords k
            LEFT JOIN p.researchTopics t
            LEFT JOIN p.journal j
            LEFT JOIN p.authors a
            WHERE
                (
                    :search IS NULL
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(k.term) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
                )
                AND (
                    :author IS NULL
                    OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :author, '%'))
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :author, '%'))
                )
                AND (
                    :keyword IS NULL
                    OR LOWER(k.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                AND (
                    :journal IS NULL
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :journal, '%'))
                )
                AND (
                    :topic IS NULL
                    OR LOWER(t.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(k.term) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
                AND (:year IS NULL OR p.year = :year)
                AND (:yearFrom IS NULL OR p.year >= :yearFrom)
                AND (:yearTo IS NULL OR p.year <= :yearTo)
            """)
    Page<ResearchPaper> searchPapersAdvanced(
            @Param("search") String search,
            @Param("author") String author,
            @Param("keyword") String keyword,
            @Param("journal") String journal,
            @Param("topic") String topic,
            @Param("year") Integer year,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            Pageable pageable);

    @Query("""
                SELECT p.year, COUNT(DISTINCT p)
                FROM ResearchPaper p
                JOIN p.keywords k
                WHERE p.year IS NOT NULL
                AND LOWER(k.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                GROUP BY p.year
                ORDER BY p.year ASC
            """)
    List<Object[]> getTrendByKeyword(@Param("keyword") String keyword);

    @Query("""
                SELECT p.year, COUNT(DISTINCT p)
                FROM ResearchPaper p
                JOIN p.researchTopics t
                WHERE p.year IS NOT NULL
                AND LOWER(t.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                GROUP BY p.year
                ORDER BY p.year ASC
            """)
    List<Object[]> getTrendByTopic(@Param("topic") String topic);

    @Query("""
                SELECT t.name, COUNT(DISTINCT p)
                FROM ResearchPaper p
                JOIN p.researchTopics t
                WHERE p.year IS NOT NULL
                AND p.year >= :fromYear
                GROUP BY t.researchTopicId, t.name
                ORDER BY COUNT(DISTINCT p) DESC
            """)
    List<Object[]> getTop5TrendingTopics(
            @Param("fromYear") Integer fromYear,
            Pageable pageable);
}
