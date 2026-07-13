package com.swp391.scientific_journal_tracker.dto.response;

import java.time.LocalDateTime;

import com.swp391.scientific_journal_tracker.entity.Bookmark;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookmarkResponse {
    private Long bookmarkId;
    private String bookmarkType;

    private Long paperId;
    private String title;
    private String authors;
    private Integer year;
    private String journalTitle;

    private Long keywordId;
    private String term;

    private LocalDateTime savedAt;

    public static BookmarkResponse fromEntity(Bookmark bookmark) {
        BookmarkResponse response = new BookmarkResponse();

        response.setBookmarkId(bookmark.getBookmarkId());
        response.setBookmarkType(bookmark.getBookmarkType());
        response.setSavedAt(bookmark.getSavedAt());

        if ("PAPER".equals(bookmark.getBookmarkType()) && bookmark.getResearchPaper() != null) {
            ResearchPaper paper = bookmark.getResearchPaper();

            response.setPaperId(paper.getResearchPaperId());
            response.setTitle(paper.getTitle());
            response.setAuthors(paper.getAuthorsRaw());
            response.setYear(paper.getYear());
            response.setJournalTitle(paper.getJournal() != null ? paper.getJournal().getTitle() : null);
        }

        if ("KEYWORD".equals(bookmark.getBookmarkType()) && bookmark.getKeyword() != null) {
            Keyword keyword = bookmark.getKeyword();

            response.setKeywordId(keyword.getKeywordId());
            response.setTerm(keyword.getTerm());
        }

        return response;
    }
}