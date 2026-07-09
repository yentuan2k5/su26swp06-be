package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.dto.request.GenerateReportRequest;
import com.swp391.scientific_journal_tracker.dto.response.*;
import com.swp391.scientific_journal_tracker.entity.DashboardReport;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.DashboardReportRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardReportService {

    private final DashboardReportRepository dashboardReportRepository;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final TrendService trendService;

    @Transactional
    public DashboardReportResponse generateReport(GenerateReportRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);
        DashboardSummaryResponse summary = dashboardService.getSummary();

        String title = normalize(request.getTitle());
        if (title == null) {
            title = "Scientific Journal Analytical Report";
        }

        String content = buildReportContent(request, summary);

        DashboardReport report = new DashboardReport();
        report.setTitle(title);
        report.setContent(content);
        report.setGeneratedAt(LocalDateTime.now());
        report.setUser(user);

        return DashboardReportResponse.fromEntity(dashboardReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<DashboardReportResponse> getMyReports(Authentication authentication) {
        User user = getCurrentUser(authentication);

        return dashboardReportRepository.findByUserUserIdOrderByGeneratedAtDesc(user.getUserId())
                .stream()
                .map(DashboardReportResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardReportResponse getMyReportDetail(Long reportId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        DashboardReport report = findReport(reportId);
        checkOwner(report, user);

        return DashboardReportResponse.fromEntity(report);
    }

    @Transactional(readOnly = true)
    public List<DashboardReportResponse> searchMyReports(String keyword, Authentication authentication) {
        User user = getCurrentUser(authentication);
        String safeKeyword = normalize(keyword);

        if (safeKeyword == null) {
            return getMyReports(authentication);
        }

        return dashboardReportRepository.findByTitleContainingIgnoreCase(safeKeyword)
                .stream()
                .filter(report -> report.getUser() != null && report.getUser().getUserId().equals(user.getUserId()))
                .map(DashboardReportResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteMyReport(Long reportId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        DashboardReport report = findReport(reportId);
        checkOwner(report, user);

        dashboardReportRepository.delete(report);
    }

    @Transactional(readOnly = true)
    public List<DashboardReportResponse> getAllReportsForAdmin() {
        return dashboardReportRepository.findAll()
                .stream()
                .map(DashboardReportResponse::fromEntity)
                .toList();
    }

    private String buildReportContent(GenerateReportRequest request, DashboardSummaryResponse summary) {
        StringBuilder content = new StringBuilder();

        content.append("SCIENTIFIC JOURNAL PUBLICATION TREND REPORT\n");
        content.append("Generated at: ").append(LocalDateTime.now()).append("\n\n");

        content.append("1. Overall statistics\n");
        content.append("- Total papers: ").append(summary.getTotalPapers()).append("\n");
        content.append("- Total journals: ").append(summary.getTotalJournals()).append("\n");
        content.append("- Total keywords: ").append(summary.getTotalKeywords()).append("\n");
        content.append("- OpenAlex papers: ").append(summary.getOpenAlexPapers()).append("\n");
        content.append("- Successful syncs: ").append(summary.getSuccessfulSyncs()).append("\n");
        content.append("- Failed syncs: ").append(summary.getFailedSyncs()).append("\n\n");

        appendChart(content, "2. Papers by year", summary.getPapersByYear());
        appendChart(content, "3. Top keywords", summary.getTopKeywords());
        appendChart(content, "4. Top journals", summary.getTopJournals());

        String keyword = normalize(request.getKeyword());
        if (keyword != null) {
            content.append("5. Keyword trend: ").append(keyword).append("\n");
            appendTrend(content, trendService.getTrendByKeyword(keyword));
        }

        String topic = normalize(request.getTopic());
        if (topic != null) {
            content.append("6. Topic trend: ").append(topic).append("\n");
            appendTrend(content, trendService.getTrendByTopic(topic));
        }

        content.append("7. Top trending topics\n");
        List<TopTopicResponse> topTopics = trendService.getTopTrendingTopics(null, 5);

        if (topTopics.isEmpty()) {
            content.append("- No trending topic data available.\n");
        } else {
            topTopics.forEach(item -> content
                    .append("- ").append(item.getTopic())
                    .append(": ").append(item.getPaperCount())
                    .append(" papers\n"));
        }

        return content.toString();
    }

    private void appendChart(StringBuilder content, String title, List<DashboardChartItemResponse> items) {
        content.append(title).append("\n");

        if (items == null || items.isEmpty()) {
            content.append("- No data available.\n\n");
            return;
        }

        items.forEach(item -> content
                .append("- ").append(item.getLabel())
                .append(": ").append(item.getValue())
                .append("\n"));

        content.append("\n");
    }

    private void appendTrend(StringBuilder content, List<TrendResponse> trend) {
        if (trend == null || trend.isEmpty()) {
            content.append("- No trend data available.\n\n");
            return;
        }

        trend.forEach(item -> content
                .append("- ").append(item.getYear())
                .append(": ").append(item.getPaperCount())
                .append(" papers\n"));

        content.append("\n");
    }

    private DashboardReport findReport(Long reportId) {
        return dashboardReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private void checkOwner(DashboardReport report, User user) {
        if (report.getUser() == null || !report.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("You do not have permission to access this report");
        }
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}