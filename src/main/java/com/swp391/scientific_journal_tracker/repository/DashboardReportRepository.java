package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.scientific_journal_tracker.entity.DashboardReport;

public interface DashboardReportRepository extends JpaRepository<DashboardReport, Long> {
    List<DashboardReport> findByUserUserId(Long userId);

    List<DashboardReport> findByTitleContainingIgnoreCase(String keyword);

    List<DashboardReport> findByUserUserIdOrderByGeneratedAtDesc(Long userId);

    Optional<DashboardReport> findByDashboardReportIdAndUserUserId(Long dashboardReportId, Long userId);

    List<DashboardReport> findByUserUserIdAndTitleContainingIgnoreCaseOrderByGeneratedAtDesc(Long userId,
            String keyword);
}
