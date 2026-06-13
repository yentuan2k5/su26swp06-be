package com.swp391.scientific_journal_tracker.dto.response;
import com.swp391.scientific_journal_tracker.entity.Keyword;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordResponse {
    private Long keywordId;
    private String term;

    public static KeywordResponse fromEntity(Keyword keyword) {
        return new KeywordResponse(
                keyword.getKeywordId(),
                keyword.getTerm()
        );
    }
}

