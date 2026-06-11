package com.swp391.scientific_journal_tracker.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.scientific_journal_tracker.entity.ApiDataSource;
@Repository
public interface ApiDataSourceRepository extends JpaRepository<ApiDataSource, Long> {
    Optional<ApiDataSource> findByName(String name);

    boolean existsByName(String name);

    List<ApiDataSource> findByNameContainingIgnoreCase(String keyword);

    Optional<ApiDataSource> findByBaseUrl(String baseUrl);
}
