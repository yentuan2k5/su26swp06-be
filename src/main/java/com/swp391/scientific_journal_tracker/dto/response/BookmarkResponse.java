package com.swp391.scientific_journal_tracker.dto.response;

import java.time.LocalDateTime;

import com.swp391.scientific_journal_tracker.entity.Bookmark;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookmarkResponse {
    private Long bookmarkId;
    private Long paperId;
    private String title;
    private String authors;
    private Integer year;
    private String journalTitle;
    private LocalDateTime savedAt;

    public static BookmarkResponse fromEntity(Bookmark bookmark) {
        ResearchPaper paper = bookmark.getResearchPaper();

        return new BookmarkResponse(
                bookmark.getBookmarkId(),
                paper.getResearchPaperId(),
                paper.getTitle(),
                paper.getAuthors(),
                paper.getYear(),
                paper.getJournal() != null ? paper.getJournal().getTitle() : null,
                bookmark.getSavedAt());
    }
}