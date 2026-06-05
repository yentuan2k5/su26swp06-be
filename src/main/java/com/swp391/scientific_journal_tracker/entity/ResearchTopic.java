package com.swp391.scientific_journal_tracker.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ManyToAny;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ResearchTopics")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResearchTopic {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "ResearchTopicId", nullable = false, unique = true)
    private Long researchTopicId;
    @Column(name = "Name", nullable = false, columnDefinition = "VARCHAR(150) COLLATE utf8mb4_unicode_ci")
    private String name;
    @Column(name = "Description", columnDefinition = "TEXT COLLATE utf8mb4_unicode_ci")
    private String description;
    @ManyToMany
    @JoinTable(name = "paper_topics", joinColumns = @JoinColumn(name = "ResearchTopicId"), inverseJoinColumns = @JoinColumn(name = "ResearchPaperId"))
    private List<ResearchPaper> researchPapers = new ArrayList<>();
    @ManyToMany(mappedBy = "followingTopics")
    private List<User> followers = new ArrayList<>();
}
