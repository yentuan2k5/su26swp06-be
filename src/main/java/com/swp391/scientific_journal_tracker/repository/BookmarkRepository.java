package com.swp391.scientific_journal_tracker.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    
    List<Bookmark> findByUserUserId(Long userId);

    
    boolean existsByUserUserIdAndResearchPaperResearchPaperId(
            Long userId,
            Long researchPaperId
    );

    
    Optional<Bookmark> findByUserUserIdAndResearchPaperResearchPaperId(
            Long userId,
            Long researchPaperId
    );
}
