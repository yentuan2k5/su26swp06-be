package com.swp391.scientific_journal_tracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.scientific_journal_tracker.entity.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByExternalId(String externalId);

    Optional<Author> findFirstByFullNameIgnoreCase(String fullName);
}
