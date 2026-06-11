package com.swp391.scientific_journal_tracker.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.DashboardReport;

@Repository
public interface DashboardReportRepository extends JpaRepository<DashboardReport, Long> {
    List<DashboardReport> findByUserUserId(Long userId);

    List<DashboardReport> findByTitleContainingIgnoreCase(String keyword);

    List<DashboardReport> findByUserUserIdOrderByGeneratedAtDesc(Long userId);
}
