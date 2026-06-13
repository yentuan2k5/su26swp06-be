package com.swp391.scientific_journal_tracker.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.Keyword;

@Repository

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

}
