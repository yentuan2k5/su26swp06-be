package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.swp391.scientific_journal_tracker.dto.response.DashboardAnalyticsResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardHomeResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardOverviewResponse;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ResearchPaperRepository paperRepository;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private ResearchTopicRepository researchTopicRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrendService trendService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void calculatesAnalyticsFromCompletedCalendarYears() {
        int latestCompleteYear = Year.now().getValue() - 1;
        int previousCompleteYear = latestCompleteYear - 1;
        ReflectionTestUtils.setField(dashboardService, "highImpactCitationThreshold", 100);

        when(paperRepository.count()).thenReturn(100L);
        when(paperRepository.sumCitationCount()).thenReturn(1_500L);
        when(paperRepository.countByCitationCountGreaterThanEqual(100)).thenReturn(12L);
        when(paperRepository.countByYear(latestCompleteYear)).thenReturn(30L);
        when(paperRepository.countByYear(previousCompleteYear)).thenReturn(20L);
        when(trendService.getTopTrendingKeywords(isNull(), org.mockito.ArgumentMatchers.eq(5)))
                .thenReturn(List.of());
        when(trendService.getTopTrendingTopics(isNull(), org.mockito.ArgumentMatchers.eq(5)))
                .thenReturn(List.of());

        DashboardAnalyticsResponse response = dashboardService.getAnalytics();

        assertEquals(1_500L, response.getTotalCitations());
        assertEquals(15.0, response.getAverageCitationsPerPaper());
        assertEquals(12L, response.getHighImpactPaperCount());
        assertEquals(latestCompleteYear, response.getLatestCompleteYear());
        assertEquals(30L, response.getLatestCompleteYearPaperCount());
        assertEquals(previousCompleteYear, response.getPreviousCompleteYear());
        assertEquals(20L, response.getPreviousCompleteYearPaperCount());
        assertEquals(0.5, response.getPublicationGrowthRate());
    }

    @Test
    void summaryDoesNotExposeOrQuerySystemOperationMetrics() {
        when(paperRepository.count()).thenReturn(100L);
        when(journalRepository.count()).thenReturn(20L);
        when(keywordRepository.count()).thenReturn(50L);
        when(researchTopicRepository.count()).thenReturn(30L);
        when(paperRepository.countPapersByYear()).thenReturn(List.of());
        when(paperRepository.countTopKeywords(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(paperRepository.countTopJournals(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(paperRepository.findTop10ByOrderByCitationCountDesc()).thenReturn(List.of());

        DashboardOverviewResponse response = dashboardService.getSummary();

        assertEquals(100L, response.getTotalPapers());
        assertEquals(20L, response.getTotalJournals());
        assertEquals(50L, response.getTotalKeywords());
        assertEquals(30L, response.getTotalTopics());
        verify(paperRepository, never()).countBySourceApi(org.mockito.ArgumentMatchers.anyString());
        verify(syncLogRepository, never()).countByStatus(org.mockito.ArgumentMatchers.any());
        verify(syncLogRepository, never()).findTopByOrderByStartedAtDesc();
    }

    @Test
    void returnsOverviewOnlyForStudentDashboardHome() {
        stubSummaryData();
        User student = User.builder().username("student").role(User.Role.STUDENT).build();
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(student));

        DashboardHomeResponse response = dashboardService.getHome("student");

        assertEquals("STUDENT", response.getRole());
        assertNotNull(response.getOverview());
        assertNull(response.getAnalytics());
        assertNull(response.getOperations());
        assertFalse(response.getCapabilities().isCanViewResearchAnalytics());
        assertFalse(response.getCapabilities().isCanGenerateBasicReport());
        assertEquals("NONE", response.getCapabilities().getMindMapAccess());
        verify(paperRepository, never()).sumCitationCount();
        verify(syncLogRepository, never()).countByStatus(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsAnalyticsButNotOperationsForLecturerDashboardHome() {
        int latestCompleteYear = Year.now().getValue() - 1;
        int previousCompleteYear = latestCompleteYear - 1;
        ReflectionTestUtils.setField(dashboardService, "highImpactCitationThreshold", 100);
        stubSummaryData();
        when(userRepository.findByUsername("lecturer"))
                .thenReturn(Optional.of(User.builder().username("lecturer").role(User.Role.LECTURER).build()));
        when(paperRepository.sumCitationCount()).thenReturn(500L);
        when(paperRepository.countByCitationCountGreaterThanEqual(100)).thenReturn(2L);
        when(paperRepository.countByYear(latestCompleteYear)).thenReturn(20L);
        when(paperRepository.countByYear(previousCompleteYear)).thenReturn(10L);
        when(trendService.getTopTrendingKeywords(isNull(), org.mockito.ArgumentMatchers.eq(5)))
                .thenReturn(List.of());
        when(trendService.getTopTrendingTopics(isNull(), org.mockito.ArgumentMatchers.eq(5)))
                .thenReturn(List.of());

        DashboardHomeResponse response = dashboardService.getHome("lecturer");

        assertEquals("LECTURER", response.getRole());
        assertNotNull(response.getAnalytics());
        assertNull(response.getOperations());
        assertTrue(response.getCapabilities().isCanGenerateBasicReport());
        assertFalse(response.getCapabilities().isCanGenerateAdvancedReport());
        assertFalse(response.getCapabilities().isCanCompareTrends());
        assertEquals("NONE", response.getCapabilities().getMindMapAccess());
        verify(syncLogRepository, never()).countByStatus(org.mockito.ArgumentMatchers.any());
    }

    private void stubSummaryData() {
        when(paperRepository.count()).thenReturn(100L);
        when(journalRepository.count()).thenReturn(20L);
        when(keywordRepository.count()).thenReturn(50L);
        when(researchTopicRepository.count()).thenReturn(30L);
        when(paperRepository.countPapersByYear()).thenReturn(List.of());
        when(paperRepository.countTopKeywords(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(paperRepository.countTopJournals(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(paperRepository.findTop10ByOrderByCitationCountDesc()).thenReturn(List.of());
    }
}
