package com.swp391.scientific_journal_tracker.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.ResearchTopic;
@Repository

public interface ResearchTopicRepository extends JpaRepository<ResearchTopic, Long> {
    Optional<ResearchTopic> findByName(String name); 

    boolean existsByName(String name);

    List<ResearchTopic> findByNameContainingIgnoreCase(String name);

    List<ResearchTopic> findByDescriptionContainingIgnoreCase(String description);

    

    
}
