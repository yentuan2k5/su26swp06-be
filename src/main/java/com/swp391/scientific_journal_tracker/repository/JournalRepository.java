package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.scientific_journal_tracker.entity.Journal;

public interface JournalRepository extends JpaRepository<Journal, Long> {
    Optional<Journal> findByTitle(String title);
    
    Optional<Journal> findByIssn(String issn);
    
    boolean existsByIssn(String issn);

    List<Journal> findByTitleContainingIgnoreCase(String keyword);

    List<Journal> findByPublisherContainingIgnoreCase(String publisher);

    List<Journal> findByFieldContainingIgnoreCase(String field);
    
    boolean existsByTitle(String title);
}
