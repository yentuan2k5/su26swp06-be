package com.swp391.scientific_journal_tracker.dto.response;

import java.util.List;

import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;

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
    private String journalTitle;
    private List<String> topics;

    public static PaperResponse fromEntity(ResearchPaper paper) {
        List<String> keywords = paper.getKeywords() == null
                ? List.of()
                : paper.getKeywords()
                        .stream()
                        .map(Keyword::getTerm)
                        .toList();

        List<String> topics = paper.getResearchTopics() == null
                ? List.of()
                : paper.getResearchTopics()
                        .stream()
                        .map(ResearchTopic::getName)
                        .toList();

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
                keywords,
                paper.getJournal() != null ? paper.getJournal().getTitle() : null,
                topics);
    }
}