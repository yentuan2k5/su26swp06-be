package com.swp391.scientific_journal_tracker.dto.response;

import com.swp391.scientific_journal_tracker.entity.Journal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalResponse {

    private Long journalId;
    private String title;
    private String issn;
    private String publisher;
    private String field;
    private Long paperCount;
    private Long followerCount;

    public static JournalResponse fromEntity(Journal journal) {
        return new JournalResponse(
                journal.getJournalId(),
                journal.getTitle(),
                journal.getIssn(),
                journal.getPublisher(),
                journal.getField(),
                journal.getResearchPapers() != null ? (long) journal.getResearchPapers().size() : 0L,
                journal.getFollowers() != null ? (long) journal.getFollowers().size() : 0L);
    }
}