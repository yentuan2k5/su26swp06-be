package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swp391.scientific_journal_tracker.dto.response.PaperComparisonResponse;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

@ExtendWith(MockitoExtension.class)
class PaperServiceTest {

    @Mock
    private ResearchPaperRepository researchPaperRepository;

    @InjectMocks
    private PaperService paperService;

    @Test
    void comparesPaperMetadataAndFindsCommonAndUniqueValues() {
        ResearchPaper firstPaper = createPaper(
                10L,
                "Transformer for NLP",
                2024,
                120,
                List.of("Artificial Intelligence", "Transformer"),
                List.of("Natural Language Processing", "Machine Learning"));
        ResearchPaper secondPaper = createPaper(
                11L,
                "Vision Foundation Models",
                2023,
                95,
                List.of("Artificial Intelligence", "Computer Vision"),
                List.of("Natural Language Processing", "Computer Vision"));

        when(researchPaperRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(firstPaper, secondPaper));

        PaperComparisonResponse response = paperService.comparePapers(List.of(10L, 11L));

        assertEquals(2, response.getPapers().size());
        assertEquals(10L, response.getPapers().getFirst().getPaper().getResearchPaperId());
        assertEquals(10L, response.getNewestPaperId());
        assertEquals(10L, response.getMostCitedPaperId());
        assertEquals(List.of("Artificial Intelligence"), response.getCommonKeywords());
        assertEquals(List.of("Natural Language Processing"), response.getCommonTopics());
        assertEquals(List.of("Transformer"), response.getPapers().getFirst().getUniqueKeywords());
        assertEquals(List.of("Computer Vision"), response.getPapers().get(1).getUniqueTopics());
        assertEquals(1, response.getSimilarities().size());
        assertEquals(0.3333, response.getSimilarities().getFirst().getKeywordSimilarity());
        assertTrue(response.getPapers().getFirst().getCitationsPerYear() > 0);
    }

    @Test
    void rejectsComparisonWithDuplicateOrTooFewPaperIds() {
        assertThrows(
                BadRequestException.class,
                () -> paperService.comparePapers(List.of(10L)));
        assertThrows(
                BadRequestException.class,
                () -> paperService.comparePapers(List.of(10L, 10L)));
    }

    private ResearchPaper createPaper(
            Long id,
            String title,
            int year,
            int citationCount,
            List<String> keywordTerms,
            List<String> topicNames) {
        ResearchPaper paper = new ResearchPaper();
        paper.setResearchPaperId(id);
        paper.setTitle(title);
        paper.setYear(year);
        paper.setCitationCount(citationCount);
        paper.setKeywords(keywordTerms.stream()
                .map(this::createKeyword)
                .toList());
        paper.setResearchTopics(topicNames.stream()
                .map(this::createTopic)
                .toList());
        return paper;
    }

    private Keyword createKeyword(String term) {
        Keyword keyword = new Keyword();
        keyword.setTerm(term);
        return keyword;
    }

    private ResearchTopic createTopic(String name) {
        ResearchTopic topic = new ResearchTopic();
        topic.setName(name);
        return topic;
    }
}
