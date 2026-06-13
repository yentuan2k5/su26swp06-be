package com.swp391.scientific_journal_tracker.dto.response;
import java.util.List;

import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperResponse {
    private Long researchPaperId;
    private String externalId;
    private String title;
    private String abstractText;
    private Integer year;
    private String doi;
    private Integer citationCount;
    private String sourceApi;
    private String authors;
    private List<String> keywords;

    public static PaperResponse fromEntity(ResearchPaper paper) {
        return new PaperResponse(
                paper.getResearchPaperId(),
                paper.getExternalId(),
                paper.getTitle(),
                paper.getAbstractText(),
                paper.getYear(),
                paper.getDoi(),
                paper.getCitationCount(),
                paper.getSourceApi(),
                paper.getAuthors(),
                paper.getKeywords()
                        .stream()
                        .map(Keyword::getTerm)
                        .toList()
        );
    }
}   
