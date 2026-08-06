package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.swp391.scientific_journal_tracker.dto.request.GenerateReportRequest;
import com.swp391.scientific_journal_tracker.entity.DashboardReport;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.repository.DashboardReportRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DashboardReportServiceTest {

    @Mock
    private DashboardReportRepository dashboardReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResearchPaperRepository researchPaperRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    @InjectMocks
    private DashboardReportService dashboardReportService;

    @Test
    void lecturerCannotRequestAdvancedReportSection() {
        User lecturer = new User();
        lecturer.setUserId(10L);
        lecturer.setUsername("lecturer");
        lecturer.setRole(User.Role.LECTURER);

        GenerateReportRequest request = new GenerateReportRequest();
        request.setSections(List.of("KEYWORD_TREND"));

        when(userRepository.findByUsername("lecturer"))
                .thenReturn(Optional.of(lecturer));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("lecturer", null);

        assertThrows(
                AccessDeniedException.class,
                () -> dashboardReportService.generateReport(request, authentication));

        verifyNoInteractions(
                dashboardReportRepository,
                researchPaperRepository,
                syncLogRepository);
    }

    @Test
    void deletesReportOnlyWhenRepositoryFindsItForCurrentOwner() {
        User owner = new User();
        owner.setUserId(10L);
        owner.setUsername("researcher");
        DashboardReport report = new DashboardReport();
        report.setDashboardReportId(99L);
        report.setUser(owner);

        when(userRepository.findByUsername("researcher")).thenReturn(Optional.of(owner));
        when(dashboardReportRepository.findByDashboardReportIdAndUserUserId(99L, 10L))
                .thenReturn(Optional.of(report));

        dashboardReportService.deleteMyReport(99L,
                new UsernamePasswordAuthenticationToken("researcher", null));

        verify(dashboardReportRepository).findByDashboardReportIdAndUserUserId(99L, 10L);
        verify(dashboardReportRepository).delete(report);
    }

    @Test
    void keywordTrendDoesNotFilterTheGeneralCatalogSummary() {
        User researcher = new User();
        researcher.setUserId(20L);
        researcher.setUsername("researcher");
        researcher.setRole(User.Role.RESEARCHER);

        GenerateReportRequest request = new GenerateReportRequest();
        request.setTitle("AI trend report");
        request.setKeyword("machine learning");
        request.setSections(List.of("OVERALL_STATISTICS", "KEYWORD_TREND"));

        when(userRepository.findByUsername("researcher")).thenReturn(Optional.of(researcher));
        when(researchPaperRepository.countReportPapers(null, null, null)).thenReturn(100L);
        when(researchPaperRepository.countReportJournals(null, null, null)).thenReturn(12L);
        when(researchPaperRepository.countReportKeywords(null, null, null)).thenReturn(25L);
        when(researchPaperRepository.countReportPapersBySource("openalex", null, null, null))
                .thenReturn(80L);
        when(researchPaperRepository.countReportPapersByYear(null, "machine learning", null))
                .thenReturn(List.<Object[]>of(new Object[] { 2025, 4L }));
        when(dashboardReportRepository.save(any(DashboardReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = dashboardReportService.generateReport(
                request,
                new UsernamePasswordAuthenticationToken("researcher", null));

        assertTrue(response.getContent().contains("Total papers: 100"));
        assertTrue(response.getContent().contains("OpenAlex papers: 80"));
        verify(researchPaperRepository).countReportPapers(null, null, null);
        verify(researchPaperRepository).countReportPapersByYear(null, "machine learning", null);
    }
}
