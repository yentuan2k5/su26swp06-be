package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.swp391.scientific_journal_tracker.dto.response.TrendAnalysisResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendComparisonResponse;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

@ExtendWith(MockitoExtension.class)
class TrendServiceTest {

    @Mock
    private ResearchPaperRepository researchPaperRepository;

    @InjectMocks
    private TrendService trendService;

    @BeforeEach
    void configureTrendThresholds() {
        ReflectionTestUtils.setField(trendService, "minPapersThreshold", 30);
        ReflectionTestUtils.setField(trendService, "minPreviousPapersThreshold", 5);
        ReflectionTestUtils.setField(trendService, "minRecentPapersForEmerging", 30);
        ReflectionTestUtils.setField(trendService, "stableGrowthRate", 0.10d);
    }

    @Test
    void comparesKeywordSeriesOnTheSameYearAxis() {
        when(researchPaperRepository.getKeywordComparisonTrends(
                eq(List.of("machine learning", "deep learning")), eq(2020), eq(2025)))
                .thenReturn(List.of(
                        new Object[] { "machine learning", 2020, 10L },
                        new Object[] { "machine learning", 2023, 10L },
                        new Object[] { "machine learning", 2025, 30L },
                        new Object[] { "deep learning", 2020, 5L },
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
        assertEquals(10L, response.getSeries().get(0).getPreviousCount());
        assertEquals(40L, response.getSeries().get(0).getRecentCount());
        assertEquals(50L, response.getSeries().get(0).getTotalPapers());
        assertEquals(3.0, response.getSeries().get(0).getGrowthRate());
        assertEquals("GROWING", response.getSeries().get(0).getTrendType());
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

    @Test
    void allowsComparisonWithFiveSeries() {
        List<String> normalizedNames = List.of("alpha", "beta", "gamma", "delta", "epsilon");
        when(researchPaperRepository.getKeywordComparisonTrends(
                eq(normalizedNames), eq(2022), eq(2023)))
                .thenReturn(List.of(
                        new Object[] { "alpha", 2023, 1L },
                        new Object[] { "beta", 2023, 1L },
                        new Object[] { "gamma", 2023, 1L },
                        new Object[] { "delta", 2023, 1L },
                        new Object[] { "epsilon", 2023, 1L }));

        TrendComparisonResponse response = trendService.compareTrends(
                "KEYWORD",
                List.of("Alpha", "Beta", "Gamma", "Delta", "Epsilon"),
                2023,
                2023);

        assertEquals(5, response.getSeries().size());
    }

    @Test
    void calculatesKeywordAnalysisFromTwoEqualPeriods() {
        when(researchPaperRepository.getTrendByKeyword("Transformer"))
                .thenReturn(List.of(
                        new Object[] { 2019, 10L },
                        new Object[] { 2020, 10L },
                        new Object[] { 2021, 15L },
                        new Object[] { 2022, 25L }));

        TrendAnalysisResponse response = trendService.analyzeKeywordTrend(
                "Transformer", 2021, 2022);

        assertEquals("KEYWORD", response.getType());
        assertEquals(2019, response.getPreviousFromYear());
        assertEquals(2020, response.getPreviousToYear());
        assertEquals(20L, response.getPreviousCount());
        assertEquals(40L, response.getRecentCount());
        assertEquals(60L, response.getTotalPapers());
        assertEquals(1.0, response.getGrowthRate());
        assertEquals("GROWING", response.getTrendType());
        assertTrue(response.isSufficientData());
        assertEquals(2, response.getYearlyData().size());
        assertEquals(2, response.getYearlyGrowthData().size());
        assertEquals(10L, response.getYearlyGrowthData().get(0).getPreviousPaperCount());
        assertEquals(15L, response.getYearlyGrowthData().get(0).getPaperCount());
        assertEquals(0.5, response.getYearlyGrowthData().get(0).getGrowthRate());
        assertEquals(15L, response.getYearlyGrowthData().get(1).getPreviousPaperCount());
        assertEquals(25L, response.getYearlyGrowthData().get(1).getPaperCount());
        assertEquals(2.0 / 3.0, response.getYearlyGrowthData().get(1).getGrowthRate());
    }
}
