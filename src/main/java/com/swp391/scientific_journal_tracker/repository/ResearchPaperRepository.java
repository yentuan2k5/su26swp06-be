package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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

}
