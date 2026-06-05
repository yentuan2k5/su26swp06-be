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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Keywords", indexes = {
        @Index(name = "idx_keyword_name", columnList = "Term")
})
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Keyword {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "KeywordId", nullable = false, unique = true)
    private Long keywordId;
    @Column(name = "Term", nullable = false, unique = true, columnDefinition = "VARCHAR(100) COLLATE utf8mb4_unicode_ci")
    private String term;
    @ManyToMany(mappedBy = "keywords")
    private List<ResearchPaper> researchPapers = new ArrayList<>();
}
