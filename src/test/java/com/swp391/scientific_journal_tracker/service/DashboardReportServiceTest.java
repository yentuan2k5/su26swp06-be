package com.swp391.scientific_journal_tracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
