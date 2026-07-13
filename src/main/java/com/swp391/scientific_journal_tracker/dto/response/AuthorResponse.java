package com.swp391.scientific_journal_tracker.dto.response;

import com.swp391.scientific_journal_tracker.entity.Author;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

    private Long authorId;
    private String fullName;
    private String externalId;
    private Long paperCount;

    public static AuthorResponse fromEntity(Author author) {
        return new AuthorResponse(
                author.getAuthorId(),
                author.getFullName(),
                author.getExternalId(),
                author.getResearchPapers() == null
                        ? 0L
                        : (long) author.getResearchPapers().size());
    }
}