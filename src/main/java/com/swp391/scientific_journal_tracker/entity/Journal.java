package com.swp391.scientific_journal_tracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Journals")
@AllArgsConstructor
@Data
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
}
