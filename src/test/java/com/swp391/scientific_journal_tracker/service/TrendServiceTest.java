package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swp391.scientific_journal_tracker.dto.response.TrendComparisonResponse;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

@ExtendWith(MockitoExtension.class)
class TrendServiceTest {

    @Mock
    private ResearchPaperRepository researchPaperRepository;

    @InjectMocks
    private TrendService trendService;

    @Test
    void comparesKeywordSeriesOnTheSameYearAxis() {
        when(researchPaperRepository.getKeywordComparisonTrends(
                eq(List.of("machine learning", "deep learning")), eq(2023), eq(2025)))
                .thenReturn(List.of(
                        new Object[] { "machine learning", 2023, 10L },
                        new Object[] { "machine learning", 2025, 30L },
                        new Object[] { "deep learning", 2023, 5L },
                        new Object[] { "deep learning", 2025, 15L }));

        TrendComparisonResponse response = trendService.compareTrends(
                "keyword",
                List.of("Machine Learning", "Deep Learning"),
                2023,
                2025);

        assertEquals("KEYWORD", response.getType());
        assertEquals(2, response.getSeries().size());
        assertEquals("Machine Learning", response.getSeries().get(0).getName());
        assertEquals(40L, response.getSeries().get(0).getTotalPapers());
        assertEquals(2.0, response.getSeries().get(0).getGrowthRate());
        assertEquals(3, response.getSeries().get(0).getYearlyData().size());
        assertEquals(0L, response.getSeries().get(0).getYearlyData().get(1).getPaperCount());
    }

    @Test
    void rejectsComparisonWithFewerThanTwoItems() {
        assertThrows(BadRequestException.class, () -> trendService.compareTrends(
                "TOPIC",
                List.of("Machine Learning"),
                2023,
                2025));
    }
}
