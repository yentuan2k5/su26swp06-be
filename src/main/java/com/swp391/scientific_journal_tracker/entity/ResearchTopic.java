package com.swp391.scientific_journal_tracker.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ResearchTopics", indexes = {
        @Index(name = "idx_research_topic_name", columnList = "Name")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResearchTopic {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "ResearchTopicId", nullable = false, unique = true)
    private Long researchTopicId;
    @Column(name = "Name", nullable = false, columnDefinition = "VARCHAR(150) COLLATE utf8mb4_unicode_ci")
    private String name;
    @Column(name = "Description", columnDefinition = "TEXT COLLATE utf8mb4_unicode_ci")
    private String description;
    @ManyToMany(mappedBy = "researchTopics")
    private List<ResearchPaper> researchPapers = new ArrayList<>();
    @ManyToMany(mappedBy = "followingTopics")
    private List<User> followers = new ArrayList<>();
}
