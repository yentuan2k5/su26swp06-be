package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.dto.request.GenerateReportRequest;
import com.swp391.scientific_journal_tracker.dto.response.*;
import com.swp391.scientific_journal_tracker.entity.DashboardReport;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.DashboardReportRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardReportService {

    private static final String SOURCE_OPENALEX = "openalex";
    private static final int REPORT_TOP_LIMIT = 10;
    private static final int DEFAULT_TREND_PERIOD_YEARS = 5;
    private static final String TREND_TYPE_GROWING = "GROWING";
    private static final String TREND_TYPE_EMERGING = "EMERGING";

    private static final String SECTION_OVERALL_STATISTICS = "OVERALL_STATISTICS";
    private static final String SECTION_PAPERS_BY_YEAR = "PAPERS_BY_YEAR";
    private static final String SECTION_TOP_KEYWORDS = "TOP_KEYWORDS";
    private static final String SECTION_TOP_JOURNALS = "TOP_JOURNALS";
    private static final String SECTION_TOP_CITED_PAPERS = "TOP_CITED_PAPERS";
    private static final String SECTION_KEYWORD_TREND = "KEYWORD_TREND";
    private static final String SECTION_TOPIC_TREND = "TOPIC_TREND";
    private static final String SECTION_TOP_TRENDING_TOPICS = "TOP_TRENDING_TOPICS";

    private static final Set<String> DEFAULT_REPORT_SECTIONS = Set.of(
            SECTION_OVERALL_STATISTICS,
            SECTION_PAPERS_BY_YEAR,
            SECTION_TOP_KEYWORDS,
            SECTION_TOP_JOURNALS,
            SECTION_TOP_CITED_PAPERS,
            SECTION_KEYWORD_TREND,
            SECTION_TOPIC_TREND,
            SECTION_TOP_TRENDING_TOPICS);

    /*
     * Lecturer chỉ được tạo báo cáo phục vụ giảng dạy/tổng quan.
     * Các section phân tích xu hướng chuyên sâu được dành cho Researcher
     * và Admin. Quy tắc này phải nằm ở service để không thể vượt quyền
     * chỉ bằng cách gọi trực tiếp REST API.
     */
    private static final Set<String> LECTURER_REPORT_SECTIONS = Set.of(
            SECTION_OVERALL_STATISTICS,
            SECTION_PAPERS_BY_YEAR,
            SECTION_TOP_KEYWORDS,
            SECTION_TOP_JOURNALS,
            SECTION_TOP_CITED_PAPERS);

    private final DashboardReportRepository dashboardReportRepository;
    private final UserRepository userRepository;
    private final ResearchPaperRepository researchPaperRepository;
    private final SyncLogRepository syncLogRepository;

    @Value("${trend.min-papers-threshold:30}")
    private int minPapersThreshold;

    @Value("${trend.min-previous-papers-threshold:5}")
    private int minPreviousPapersThreshold;

    @Value("${trend.min-recent-papers-for-emerging:30}")
    private int minRecentPapersForEmerging;

    @Value("${trend.excluded-keywords:computer science}")
    private String excludedKeywords;

    @Transactional
    public DashboardReportResponse generateReport(GenerateReportRequest request, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Set<String> sections = resolveSections(request, user);
        Integer fromYear = resolveFromYear(request.getTimeHorizonYears());
        String keyword = normalize(request.getKeyword());
        String topic = normalize(request.getTopic());
        DashboardSummaryResponse summary = buildReportSummary(
                keyword,
                topic,
                fromYear,
                sections);
        DashboardReportChartsResponse charts = buildReportCharts(
                keyword,
                topic,
                summary,
                sections,
                fromYear);

        String title = normalize(request.getTitle());
        if (title == null) {
            title = "Scientific Journal Analytical Report";
        }

        String content = buildReportContent(
                request,
                summary,
                charts,
                sections,
                fromYear);

        DashboardReport report = new DashboardReport();
        report.setTitle(title);
        report.setContent(content);
        report.setGeneratedAt(LocalDateTime.now());
        report.setUser(user);

        return DashboardReportResponse.fromEntity(
                dashboardReportRepository.save(report),
                charts);
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
        DashboardReport report = findMyReport(reportId, user);

        return DashboardReportResponse.fromEntity(
                report,
                buildReportChartsFromContent(report.getContent()));
    }

    @Transactional(readOnly = true)
    public List<DashboardReportResponse> searchMyReports(String keyword, Authentication authentication) {
        User user = getCurrentUser(authentication);
        String safeKeyword = normalize(keyword);

        if (safeKeyword == null) {
            return getMyReports(authentication);
        }

        return dashboardReportRepository
                .findByUserUserIdAndTitleContainingIgnoreCaseOrderByGeneratedAtDesc(user.getUserId(), safeKeyword)
                .stream()
                .map(DashboardReportResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteMyReport(Long reportId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        DashboardReport report = findMyReport(reportId, user);

        dashboardReportRepository.delete(report);
    }

    @Transactional(readOnly = true)
    public List<DashboardReportResponse> getAllReportsForAdmin() {
        return dashboardReportRepository.findAll()
                .stream()
                .map(DashboardReportResponse::fromEntity)
                .toList();
    }

    private String buildReportContent(
            GenerateReportRequest request,
            DashboardSummaryResponse summary,
            DashboardReportChartsResponse charts,
            Set<String> sections,
            Integer fromYear) {
        StringBuilder content = new StringBuilder();
        int sectionNumber = 1;

        content.append("SCIENTIFIC JOURNAL PUBLICATION TREND REPORT\n");
        content.append("Generated at: ").append(LocalDateTime.now()).append("\n\n");
        content.append("Time horizon: ").append(formatTimeHorizon(fromYear)).append("\n");
        content.append("Preferred format: ").append(resolveFormat(request.getFormat())).append("\n");
        content.append("Selected sections: ").append(formatSections(sections)).append("\n\n");

        String keyword = normalize(request.getKeyword());
        String topic = normalize(request.getTopic());
        if (keyword != null) {
            content.append("Keyword filter: ").append(keyword).append("\n");
        }
        if (topic != null) {
            content.append("Topic filter: ").append(topic).append("\n");
        }
        if (keyword != null || topic != null) {
            content.append("\n");
        }

        if (sections.contains(SECTION_OVERALL_STATISTICS)) {
            content.append(sectionNumber++).append(". Overall statistics\n");
            content.append("- Total papers: ").append(summary.getTotalPapers()).append("\n");
            content.append("- Total journals: ").append(summary.getTotalJournals()).append("\n");
            content.append("- Total keywords: ").append(summary.getTotalKeywords()).append("\n");
            content.append("- OpenAlex papers: ").append(summary.getOpenAlexPapers()).append("\n");
            if (keyword == null && topic == null) {
                content.append("- Successful syncs: ").append(summary.getSuccessfulSyncs()).append("\n");
                content.append("- Failed syncs: ").append(summary.getFailedSyncs()).append("\n");
            }
            content.append("\n");
        }

        if (sections.contains(SECTION_PAPERS_BY_YEAR)) {
            appendChart(content, sectionNumber++ + ". Papers by year", charts.getPapersByYear());
        }

        if (sections.contains(SECTION_TOP_KEYWORDS)) {
            appendChart(content, sectionNumber++ + ". Top keywords", charts.getTopKeywords());
        }

        if (sections.contains(SECTION_TOP_JOURNALS)) {
            appendChart(content, sectionNumber++ + ". Top journals", charts.getTopJournals());
        }

        if (sections.contains(SECTION_TOP_CITED_PAPERS)) {
            appendTopCitedPapers(content, sectionNumber++ + ". Top cited papers", charts.getTopCitedPapers());
        }

        if (sections.contains(SECTION_KEYWORD_TREND) && keyword != null) {
            content.append(sectionNumber++).append(". Keyword trend: ").append(keyword).append("\n");
            appendTrend(content, charts.getKeywordTrend());
        }

        if (sections.contains(SECTION_TOPIC_TREND) && topic != null) {
            content.append(sectionNumber++).append(". Topic trend: ").append(topic).append("\n");
            appendTrend(content, charts.getTopicTrend());
        }

        if (sections.contains(SECTION_TOP_TRENDING_TOPICS)) {
            content.append(sectionNumber).append(". Top trending topics\n");
            List<TopTopicResponse> topTopics = charts.getTopTrendingTopics();

            if (topTopics.isEmpty()) {
                content.append("- No trending topic data available.\n");
            } else {
                topTopics.forEach(item -> content
                        .append("- ").append(item.getTopic())
                        .append(": ").append(item.getPaperCount())
                        .append(" papers\n"));
            }
        }

        return content.toString();
    }

    private DashboardReportChartsResponse buildReportChartsFromContent(String content) {
        Set<String> sections = extractReportSections(content);
        Integer fromYear = extractReportFromYear(content);
        String keyword = extractReportFilter(content, "Keyword filter: ");
        String topic = extractReportFilter(content, "Topic filter: ");

        // Tương thích các report cũ chưa lưu filter ở phần header.
        if (keyword == null) {
            keyword = extractReportFilter(content, "Keyword trend: ");
        }
        if (topic == null) {
            topic = extractReportFilter(content, "Topic trend: ");
        }

        return buildReportCharts(
                keyword,
                topic,
                buildReportSummary(
                        keyword,
                        topic,
                        fromYear,
                        sections),
                sections,
                fromYear);
    }

    private DashboardSummaryResponse buildReportSummary(
            String keyword,
            String topic,
            Integer fromYear,
            Set<String> sections) {
        PageRequest topLimit = PageRequest.of(0, REPORT_TOP_LIMIT);
        PageRequest keywordFetchLimit = PageRequest.of(0, REPORT_TOP_LIMIT * 3);
        boolean needsOverallStatistics = sections.contains(SECTION_OVERALL_STATISTICS);
        boolean needsPapersByYear = sections.contains(SECTION_PAPERS_BY_YEAR);
        boolean needsTopKeywords = sections.contains(SECTION_TOP_KEYWORDS);
        boolean needsTopJournals = sections.contains(SECTION_TOP_JOURNALS);
        boolean needsTopCitedPapers = sections.contains(SECTION_TOP_CITED_PAPERS);

        return new DashboardSummaryResponse(
                needsOverallStatistics
                        ? researchPaperRepository.countReportPapers(
                                fromYear,
                                keyword,
                                topic)
                        : 0,
                needsOverallStatistics
                        ? researchPaperRepository.countReportJournals(
                                fromYear,
                                keyword,
                                topic)
                        : 0,
                needsOverallStatistics
                        ? researchPaperRepository.countReportKeywords(
                                fromYear,
                                keyword,
                                topic)
                        : 0,
                needsOverallStatistics
                        ? researchPaperRepository.countReportPapersBySource(
                                SOURCE_OPENALEX,
                                fromYear,
                                keyword,
                                topic)
                        : 0,
                needsOverallStatistics
                        ? syncLogRepository.countByStatus(Status.SUCCESS)
                        : 0,
                needsOverallStatistics
                        ? syncLogRepository.countByStatus(Status.FAILED)
                        : 0,
                needsPapersByYear
                        ? toChartItems(researchPaperRepository.countReportPapersByYear(
                                fromYear,
                                keyword,
                                topic))
                        : List.of(),
                needsTopKeywords
                        ? filterReportTopKeywords(
                                toChartItems(researchPaperRepository.countReportTopKeywords(
                                        fromYear,
                                        keyword,
                                        topic,
                                        keywordFetchLimit)),
                                keyword)
                        : List.of(),
                needsTopJournals
                        ? toChartItems(researchPaperRepository.countReportTopJournals(
                                fromYear,
                                keyword,
                                topic,
                                topLimit))
                        : List.of(),
                needsTopCitedPapers
                        ? researchPaperRepository.findReportTopCitedPapers(
                                fromYear,
                                keyword,
                                topic,
                                topLimit)
                                .stream()
                                .map(PaperResponse::fromEntity)
                                .toList()
                        : List.of(),
                null);
    }

    private DashboardReportChartsResponse buildReportCharts(
            String keyword,
            String topic,
            DashboardSummaryResponse summary,
            Set<String> sections,
            Integer fromYear) {
        return new DashboardReportChartsResponse(
                sections.contains(SECTION_PAPERS_BY_YEAR)
                        ? filterChartItemsByYear(summary.getPapersByYear(), fromYear)
                        : List.of(),
                sections.contains(SECTION_TOP_KEYWORDS)
                        ? summary.getTopKeywords()
                        : List.of(),
                sections.contains(SECTION_TOP_JOURNALS)
                        ? summary.getTopJournals()
                        : List.of(),
                sections.contains(SECTION_TOP_CITED_PAPERS)
                        ? summary.getTopCitedPapers()
                        : List.of(),
                sections.contains(SECTION_KEYWORD_TREND) && keyword != null
                        ? getFilteredReportTrend(
                                keyword,
                                null,
                                fromYear)
                        : List.of(),
                sections.contains(SECTION_TOPIC_TREND) && topic != null
                        ? getFilteredReportTrend(
                                null,
                                topic,
                                fromYear)
                        : List.of(),
                sections.contains(SECTION_TOP_TRENDING_TOPICS)
                        ? getReportTopTrendingTopics(
                                keyword,
                                topic,
                                fromYear,
                                5)
                        : List.of());
    }

    private Set<String> resolveSections(
            GenerateReportRequest request,
            User user) {
        boolean usesDefaultSections = request.getSections() == null
                || request.getSections().isEmpty();

        Set<String> requestedSections = usesDefaultSections
                ? DEFAULT_REPORT_SECTIONS
                : request.getSections()
                        .stream()
                        .map(this::normalizeSection)
                        .filter(DEFAULT_REPORT_SECTIONS::contains)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedSections.isEmpty()) {
            requestedSections = DEFAULT_REPORT_SECTIONS;
            usesDefaultSections = true;
        }

        return resolveSectionsForRole(
                requestedSections,
                usesDefaultSections,
                user.getRole());
    }

    /**
     * Áp dụng quyền ở tầng business thay vì chỉ ẩn nút ở frontend.
     * Lecturer dùng default sẽ nhận bộ section cơ bản. Nếu chủ động gửi
     * section nâng cao qua API thì trả 403 để quyền không thể bị vượt qua.
     */
    private Set<String> resolveSectionsForRole(
            Set<String> requestedSections,
            boolean usesDefaultSections,
            User.Role role) {
        if (role == User.Role.ADMIN || role == User.Role.RESEARCHER) {
            return requestedSections;
        }

        if (role == User.Role.LECTURER) {
            if (usesDefaultSections) {
                return LECTURER_REPORT_SECTIONS;
            }

            Set<String> forbiddenSections = requestedSections.stream()
                    .filter(section -> !LECTURER_REPORT_SECTIONS.contains(section))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (!forbiddenSections.isEmpty()) {
                throw new AccessDeniedException(
                        "Lecturer không có quyền dùng section nâng cao: "
                                + String.join(", ", forbiddenSections));
            }

            return requestedSections;
        }

        throw new AccessDeniedException(
                "Role hiện tại không có quyền tạo analytical report");
    }

    private String normalizeSection(String section) {
        if (section == null || section.isBlank()) {
            return "";
        }

        return section.trim()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
    }

    private Integer resolveFromYear(Integer timeHorizonYears) {
        if (timeHorizonYears == null) {
            return null;
        }

        int safeYears = Math.max(1, Math.min(timeHorizonYears, 30));
        return Year.now().getValue() - safeYears + 1;
    }

    private String formatTimeHorizon(Integer fromYear) {
        if (fromYear == null) {
            return "All available years";
        }

        return "From " + fromYear + " to " + Year.now().getValue();
    }

    private String resolveFormat(String format) {
        String safeFormat = normalize(format);
        return safeFormat == null
                ? "TXT"
                : safeFormat.toUpperCase(Locale.ROOT);
    }

    private String formatSections(Set<String> sections) {
        return sections.stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private List<DashboardChartItemResponse> filterChartItemsByYear(
            List<DashboardChartItemResponse> items,
            Integer fromYear) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        if (fromYear == null) {
            return items;
        }

        return items.stream()
                .filter(item -> {
                    try {
                        return Integer.parseInt(item.getLabel()) >= fromYear;
                    } catch (NumberFormatException ex) {
                        return true;
                    }
                })
                .toList();
    }

    // private List<TrendResponse> filterTrendByYear(
    // List<TrendResponse> trend,
    // Integer fromYear) {
    // if (trend == null || trend.isEmpty()) {
    // return List.of();
    // }

    // if (fromYear == null) {
    // return trend;
    // }

    // return trend.stream()
    // .filter(item -> item.getYear() >= fromYear)
    // .toList();
    // }

    private List<TrendResponse> getFilteredReportTrend(
            String keyword,
            String topic,
            Integer fromYear) {
        return researchPaperRepository.countReportPapersByYear(
                fromYear,
                keyword,
                topic)
                .stream()
                .map(row -> new TrendResponse(
                        (Integer) row[0],
                        ((Number) row[1]).longValue()))
                .toList();
    }

    private List<DashboardChartItemResponse> toChartItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new DashboardChartItemResponse(
                        row[0] == null ? "Unknown" : row[0].toString(),
                        ((Number) row[1]).longValue()))
                .toList();
    }

    private List<DashboardChartItemResponse> filterReportTopKeywords(
            List<DashboardChartItemResponse> items,
            String selectedKeyword) {
        Set<String> safeExcludedKeywords = parseExcludedKeywords();
        String normalizedSelectedKeyword = normalizeKeyword(selectedKeyword);

        return items.stream()
                .filter(item -> {
                    String normalizedLabel = normalizeKeyword(item.getLabel());
                    return !normalizedLabel.isBlank()
                            && !safeExcludedKeywords.contains(normalizedLabel)
                            && !normalizedLabel.equals(normalizedSelectedKeyword);
                })
                .limit(REPORT_TOP_LIMIT)
                .toList();
    }

    private Set<String> parseExcludedKeywords() {
        if (excludedKeywords == null || excludedKeywords.isBlank()) {
            return Set.of();
        }

        return java.util.Arrays.stream(excludedKeywords.split(","))
                .map(this::normalizeKeyword)
                .filter(keyword -> !keyword.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);
    }

    private List<TopTopicResponse> getReportTopTrendingTopics(
            String keyword,
            String topic,
            Integer fromYear,
            int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        int currentYear = Year.now().getValue();
        int recentStartYear = fromYear == null
                ? currentYear - DEFAULT_TREND_PERIOD_YEARS + 1
                : fromYear;
        int periodYears = currentYear - recentStartYear + 1;
        int previousEndYear = recentStartYear - 1;
        int previousStartYear = recentStartYear - periodYears;
        int safeMinPapersThreshold = Math.max(0, minPapersThreshold);

        return researchPaperRepository
                .getReportTopicGrowthStats(
                        recentStartYear,
                        previousStartYear,
                        previousEndYear,
                        keyword,
                        topic)
                .stream()
                .map(this::toTopTopicResponse)
                .filter(response -> response.getTotalPapers() >= safeMinPapersThreshold)
                .sorted((left, right) -> Double.compare(
                        right.getScore(),
                        left.getScore()))
                .limit(safeLimit)
                .toList();
    }

    private TopTopicResponse toTopTopicResponse(Object[] row) {
        long recentCount = ((Number) row[1]).longValue();
        long previousCount = ((Number) row[2]).longValue();
        long totalPapers = recentCount + previousCount;
        double growthRate = calculateGrowthRate(
                recentCount,
                previousCount);
        double score = calculateTrendScore(
                growthRate,
                totalPapers);

        return new TopTopicResponse(
                (String) row[0],
                recentCount,
                growthRate,
                totalPapers,
                score,
                resolveTrendType(
                        recentCount,
                        previousCount));
    }

    private String resolveTrendType(
            long recentCount,
            long previousCount) {
        int safeMinPreviousPapersThreshold = Math.max(
                0,
                minPreviousPapersThreshold);
        int safeMinRecentPapersForEmerging = Math.max(
                1,
                minRecentPapersForEmerging);

        if (previousCount < safeMinPreviousPapersThreshold
                && recentCount >= safeMinRecentPapersForEmerging) {
            return TREND_TYPE_EMERGING;
        }

        return TREND_TYPE_GROWING;
    }

    private double calculateGrowthRate(
            long recentCount,
            long previousCount) {
        if (previousCount == 0) {
            return recentCount > 0 ? 1.0 : 0.0;
        }

        return (double) (recentCount - previousCount) / previousCount;
    }

    private double calculateTrendScore(
            double growthRate,
            long totalPapers) {
        return growthRate * Math.log1p(totalPapers);
    }

    private Set<String> extractReportSections(String content) {
        if (content == null || content.isBlank()) {
            return DEFAULT_REPORT_SECTIONS;
        }

        Set<String> sections = new LinkedHashSet<>();
        String lowerContent = content.toLowerCase(Locale.ROOT);

        if (lowerContent.contains("overall statistics")) {
            sections.add(SECTION_OVERALL_STATISTICS);
        }
        if (lowerContent.contains("papers by year")) {
            sections.add(SECTION_PAPERS_BY_YEAR);
        }
        if (lowerContent.contains("top keywords")) {
            sections.add(SECTION_TOP_KEYWORDS);
        }
        if (lowerContent.contains("top journals")) {
            sections.add(SECTION_TOP_JOURNALS);
        }
        if (lowerContent.contains("top cited papers")) {
            sections.add(SECTION_TOP_CITED_PAPERS);
        }
        if (lowerContent.contains("keyword trend")) {
            sections.add(SECTION_KEYWORD_TREND);
        }
        if (lowerContent.contains("topic trend")) {
            sections.add(SECTION_TOPIC_TREND);
        }
        if (lowerContent.contains("top trending topics")) {
            sections.add(SECTION_TOP_TRENDING_TOPICS);
        }

        return sections.isEmpty()
                ? DEFAULT_REPORT_SECTIONS
                : sections;
    }

    private Integer extractReportFromYear(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        for (String line : content.split("\\R")) {
            String normalizedLine = line.trim().toLowerCase(Locale.ROOT);
            if (!normalizedLine.startsWith("time horizon: from ")) {
                continue;
            }

            String yearText = normalizedLine
                    .replace("time horizon: from ", "")
                    .split(" ")[0];

            try {
                return Integer.parseInt(yearText);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    private String extractReportFilter(String content, String marker) {
        if (content == null || content.isBlank()) {
            return null;
        }

        for (String line : content.split("\\R")) {
            int markerIndex = line.indexOf(marker);
            if (markerIndex >= 0) {
                return normalize(line.substring(markerIndex + marker.length()));
            }
        }

        return null;
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

    private void appendTopCitedPapers(
            StringBuilder content,
            String title,
            List<PaperResponse> papers) {
        content.append(title).append("\n");

        if (papers == null || papers.isEmpty()) {
            content.append("- No cited paper data available.\n\n");
            return;
        }

        papers.forEach(paper -> content
                .append("- ").append(paper.getTitle())
                .append(" (").append(paper.getYear() == null ? "Unknown year" : paper.getYear()).append(")")
                .append(": ").append(paper.getCitationCount() == null ? 0 : paper.getCitationCount())
                .append(" citations\n"));

        content.append("\n");
    }

    /**
     * Ownership được đưa thẳng vào điều kiện query để report của người khác
     * không bị nạp vào persistence context chỉ vì client đoán được ID.
     */
    private DashboardReport findMyReport(Long reportId, User user) {
        return dashboardReportRepository
                .findByDashboardReportIdAndUserUserId(reportId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
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
