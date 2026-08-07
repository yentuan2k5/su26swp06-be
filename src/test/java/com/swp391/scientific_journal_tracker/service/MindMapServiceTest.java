package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.swp391.scientific_journal_tracker.dto.response.MindMapResponse;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;

@ExtendWith(MockitoExtension.class)
class MindMapServiceTest {

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private ResearchTopicRepository researchTopicRepository;

    @Mock
    private ResearchPaperRepository researchPaperRepository;

    @InjectMocks
    private MindMapService mindMapService;

    @Test
    void buildsKeywordMindMapWithRelatedNodes() {
        Keyword keyword = new Keyword();
        keyword.setKeywordId(7L);
        keyword.setTerm("Transformer");

        when(keywordRepository.findById(7L)).thenReturn(Optional.of(keyword));
        when(researchPaperRepository.getKeywordMindMapStats(
                eq(7L), any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class)))
                .thenReturn(new Object[] { 40L, 30L, 10L });
        when(researchPaperRepository.findMindMapTopicsForKeyword(
                eq(7L), any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[] { 11L, "Natural Language Processing", 20L, 15L, 5L }));
        when(researchPaperRepository.findMindMapKeywordsForKeyword(
                eq(7L), any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(researchPaperRepository.findMindMapJournalsForKeyword(
                eq(7L), any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class), any(Pageable.class)))
                .thenReturn(List.of());

        MindMapResponse response = mindMapService.getMindMap("keyword", 7L, 5);

        assertEquals("KEYWORD:7", response.getRoot().getId());
        assertEquals("GROWING", response.getRoot().getTrendStatus());
        assertEquals(2, response.getNodes().size());
        assertEquals("RELATED_TOPIC", response.getEdges().getFirst().getRelation());
    }

    @Test
    void rejectsUnsupportedRootType() {
        assertThrows(
                BadRequestException.class,
                () -> mindMapService.getMindMap("JOURNAL", 1L, 5));
    }

}
