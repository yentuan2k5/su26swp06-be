package com.swp391.scientific_journal_tracker.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ResearchPapers") // Table name in plural form
@AllArgsConstructor // Constructor with all fields
@NoArgsConstructor // Default constructor
@Data // Getters, Setters, toString, equals, and hashCode
public class ResearchPaper {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "ResearchPaperId", nullable = false, unique = true)
    private Long researchPaperId;
    @Column(name = "ExternalId", unique = true, columnDefinition = "VARCHAR(100) COLLATE utf8mb4_unicode_ci")
    private String externalId; // ID from external data source
    @Column(name = "Title", nullable = false, columnDefinition = "VARCHAR(500) COLLATE utf8mb4_unicode_ci")
    private String title;
    @Column(name = "Abstract", columnDefinition = "TEXT COLLATE utf8mb4_unicode_ci")
    private String abstractText; // 'abstract' is a reserved keyword in Java
    @Column(name = "Year")
    private Integer year;
    @Column(name = "Doi", columnDefinition = "VARCHAR(200) COLLATE utf8mb4_unicode_ci")
    private String doi; // Digital Object Identifier
    @Column(name = "CitationCount", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer citationCount = 0;// Default to 0 if not provided
    @Column(name = "SourceApi", nullable = false, columnDefinition = "VARCHAR(50) COLLATE utf8mb4_unicode_ci")
    private String sourceApi = "semantic_scholar"; // URL to the paper, if available
    @Column(name = "Authors", columnDefinition = "VARCHAR(1000) COLLATE utf8mb4_unicode_ci")
    private String authors;
    @Column(name = "JournalId")
    private Long journalId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JournalId", insertable = false, updatable = false)
    private Journal journal;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApiDataSourceId", insertable = false, updatable = false)
    private ApiDataSource apiDataSource;
    @ManyToMany
    @JoinTable(name = "paper_keywords", joinColumns = @JoinColumn(name = "ResearchPaperId"), inverseJoinColumns = @JoinColumn(name = "KeywordId"))
    private List<Keyword> keywords = new ArrayList<>();
    @ManyToMany(mappedBy = "researchPapers")
    private List<ResearchTopic> researchTopics = new ArrayList<>();
    @OneToMany(mappedBy = "researchPaper")
    private List<Bookmark> bookmarks = new ArrayList<>();
}
