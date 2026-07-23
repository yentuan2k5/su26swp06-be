package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp391.scientific_journal_tracker.entity.ResearchTopic;

public interface ResearchTopicRepository extends JpaRepository<ResearchTopic, Long> {

    Optional<ResearchTopic> findByName(String name);

    Optional<ResearchTopic> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    List<ResearchTopic> findByNameContainingIgnoreCase(String name);

    List<ResearchTopic> findByDescriptionContainingIgnoreCase(String description);

    @Query("""
            SELECT DISTINCT t
            FROM ResearchTopic t
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY t.name ASC
            """)
    List<ResearchTopic> searchTopics(@Param("keyword") String keyword);

    @Query("""
            SELECT
                t.researchTopicId,
                t.name,
                t.description,
                COUNT(DISTINCT p),
                COUNT(DISTINCT f)
            FROM ResearchTopic t
            LEFT JOIN t.researchPapers p
            LEFT JOIN t.followers f
            GROUP BY t.researchTopicId, t.name, t.description
            ORDER BY t.name ASC
            """)
    List<Object[]> findAllTopicSummaries();

    @Query("""
            SELECT
                t.researchTopicId,
                t.name,
                t.description,
                COUNT(DISTINCT p),
                COUNT(DISTINCT f)
            FROM ResearchTopic t
            LEFT JOIN t.researchPapers p
            LEFT JOIN t.followers f
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            GROUP BY t.researchTopicId, t.name, t.description
            ORDER BY t.name ASC
            """)
    List<Object[]> searchTopicSummaries(@Param("keyword") String keyword);

    @Query("""
            SELECT
                t.researchTopicId,
                t.name,
                t.description,
                COUNT(DISTINCT p),
                COUNT(DISTINCT f)
            FROM ResearchTopic t
            LEFT JOIN t.researchPapers p
            LEFT JOIN t.followers f
            WHERE t.researchTopicId = :topicId
            GROUP BY t.researchTopicId, t.name, t.description
            """)
    List<Object[]> findTopicSummaryById(@Param("topicId") Long topicId);

    @Query("""
            SELECT
                t.researchTopicId,
                t.name,
                t.description,
                COUNT(DISTINCT p),
                COUNT(DISTINCT f)
            FROM ResearchTopic t
            JOIN t.followers u
            LEFT JOIN t.researchPapers p
            LEFT JOIN t.followers f
            WHERE u.userId = :userId
            GROUP BY t.researchTopicId, t.name, t.description
            ORDER BY t.name ASC
            """)
    List<Object[]> findFollowingTopicSummaries(@Param("userId") Long userId);

    @Query("""
            SELECT
                t.researchTopicId,
                t.name,
                t.description,
                COUNT(DISTINCT p),
                COUNT(DISTINCT f)
            FROM ResearchTopic t
            LEFT JOIN t.researchPapers p
            LEFT JOIN t.followers f
            GROUP BY t.researchTopicId, t.name, t.description
            ORDER BY COUNT(DISTINCT p) DESC
            """)
    List<Object[]> findPopularTopics(Pageable pageable);
}
