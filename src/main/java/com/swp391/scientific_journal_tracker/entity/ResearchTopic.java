package com.swp391.scientific_journal_tracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ResearchTopics")
public class ResearchTopic {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "ResearchTopicId", nullable = false, unique = true)
    private Long researchTopicId;
    @Column(name = "Name", nullable = false, columnDefinition = "VARCHAR(150) COLLATE utf8mb4_unicode_ci")
    private String name;
    @Column(name = "Description", columnDefinition = "TEXT COLLATE utf8mb4_unicode_ci")
    private String description;
}
