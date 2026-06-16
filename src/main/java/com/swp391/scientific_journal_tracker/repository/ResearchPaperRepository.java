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

    @Query(value = """
                SELECT DISTINCT p
                FROM ResearchPaper p
                LEFT JOIN p.keywords k
                LEFT JOIN p.researchTopics t
                LEFT JOIN p.journal j
                WHERE
                    (:search IS NULL
                        OR LOWER(p.title)        LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(p.authors)      LIKE LOWER(CONCAT('%', :search, '%')))
                    AND (:author   IS NULL OR LOWER(p.authors)   LIKE LOWER(CONCAT('%', :author,   '%')))
                    AND (:keyword  IS NULL OR LOWER(k.term)      LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (:journal  IS NULL OR LOWER(j.title)     LIKE LOWER(CONCAT('%', :journal,  '%')))
                    AND (:topic    IS NULL OR LOWER(t.name)      LIKE LOWER(CONCAT('%', :topic,    '%')))
                    AND (:yearFrom IS NULL OR p.year >= :yearFrom)
                    AND (:yearTo   IS NULL OR p.year <= :yearTo)
            """, countQuery = """
                SELECT COUNT(DISTINCT p)
                FROM ResearchPaper p
                LEFT JOIN p.keywords k
                LEFT JOIN p.researchTopics t
                LEFT JOIN p.journal j
                WHERE
                    (:search IS NULL
                        OR LOWER(p.title)        LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(p.authors)      LIKE LOWER(CONCAT('%', :search, '%')))
                    AND (:author   IS NULL OR LOWER(p.authors)   LIKE LOWER(CONCAT('%', :author,   '%')))
                    AND (:keyword  IS NULL OR LOWER(k.term)      LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (:journal  IS NULL OR LOWER(j.title)     LIKE LOWER(CONCAT('%', :journal,  '%')))
                    AND (:topic    IS NULL OR LOWER(t.name)      LIKE LOWER(CONCAT('%', :topic,    '%')))
                    AND (:yearFrom IS NULL OR p.year >= :yearFrom)
                    AND (:yearTo   IS NULL OR p.year <= :yearTo)
            """)
    Page<ResearchPaper> searchPapers(
            @Param("search") String search,
            @Param("author") String author,
            @Param("keyword") String keyword,
            @Param("journal") String journal,
            @Param("topic") String topic,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            Pageable pageable);
}
