package com.swp391.scientific_journal_tracker.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Journals")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class Journal {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "JournalId", nullable = false, unique = true)
    private Long journalId;
    @Column(name = "Title", nullable = false, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    private String title;
    @Column(name = "ISSN", nullable = false, unique = true, columnDefinition = "VARCHAR(20) COLLATE utf8mb4_unicode_ci")
    private String issn;
    @Column(name = "Publisher", columnDefinition = "VARCHAR(150) COLLATE utf8mb4_unicode_ci")
    private String publisher;
    @Column(name = "Field", columnDefinition = "VARCHAR(100) COLLATE utf8mb4_unicode_ci")
    private String field;
    @OneToMany(mappedBy = "journal", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<ResearchPaper> researchPapers = new ArrayList<>();
    @ManyToMany(mappedBy = "followingJournals")
    private List<User> followers = new ArrayList<>();
}
