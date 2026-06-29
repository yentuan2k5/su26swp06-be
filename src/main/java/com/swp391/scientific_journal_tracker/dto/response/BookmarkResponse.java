package com.swp391.scientific_journal_tracker.dto.response;

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
}
