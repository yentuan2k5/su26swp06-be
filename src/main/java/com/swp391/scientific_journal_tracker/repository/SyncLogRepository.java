package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.scientific_journal_tracker.entity.SyncLog;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    List<SyncLog> findByStatus(Status status);

    List<SyncLog> findBySourceApi(String sourceApi);

    List<SyncLog> findBySourceApiOrderByStartedAtDesc(String sourceApi);

    List<SyncLog> findAllByOrderByStartedAtDesc();

    Optional<SyncLog> findTopByOrderByStartedAtDesc();

    long countByStatus(Status status);
}
