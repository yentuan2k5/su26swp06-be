package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.scientific_journal_tracker.entity.Keyword;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByTerm(String term);

    List<Keyword> findByTermContainingIgnoreCase(String term);

    List<Keyword> findByTermContainingIgnoreCaseOrderByTermAsc(String term, Pageable pageable);

    boolean existsByTerm(String term);
}
