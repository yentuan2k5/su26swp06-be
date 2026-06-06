package com.swp391.scientific_journal_tracker.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
     // Lấy tất cả bookmark của 1 user
    List<Bookmark> findByUserUserId(Long userId);

    // Kiểm tra user đã bookmark bài báo chưa
    boolean existsByUserUserIdAndResearchPaperResearchPaperId(
            Long userId,
            Long researchPaperId
    );

    // Tìm bookmark cụ thể
    Optional<Bookmark> findByUserUserIdAndResearchPaperResearchPaperId(
            Long userId,
            Long researchPaperId
    );
}
