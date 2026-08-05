package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.DashboardAnalyticsResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardChartItemResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardOverviewResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardOperationsResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String SOURCE_OPENALEX = "openalex";
    private static final int TOP_CHART_LIMIT = 10;

    private final ResearchPaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;
    private final ResearchTopicRepository researchTopicRepository;
    private final SyncLogRepository syncLogRepository;
    private final TrendService trendService;

    @Value("${dashboard.high-impact-citation-threshold:100}")
    private int highImpactCitationThreshold;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getSummary() {
        return new DashboardOverviewResponse(
                paperRepository.count(),
                journalRepository.count(),
                keywordRepository.count(),
                researchTopicRepository.count(),
                toChartItems(paperRepository.countPapersByYear()),
                toChartItems(paperRepository.countTopKeywords(PageRequest.of(0, TOP_CHART_LIMIT))),
                toChartItems(paperRepository.countTopJournals(PageRequest.of(0, TOP_CHART_LIMIT))),
                paperRepository.findTop10ByOrderByCitationCountDesc()
                        .stream()
                        .map(PaperResponse::fromEntity)
                        .toList()
        );
    }

    /**
     * Trả về dữ liệu analytics cho dashboard nghiên cứu.
     *
     * Tăng trưởng công bố sử dụng hai năm lịch hoàn chỉnh gần nhất, thay vì năm
     * hiện tại có thể mới có dữ liệu chưa đầy đủ. Citation impact và danh sách
     * trending đều được tính động từ catalog hiện có.
     */
    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getAnalytics() {
        long totalPapers = paperRepository.count();
        long totalCitations = safeLong(paperRepository.sumCitationCount());
        int latestCompleteYear = Year.now().getValue() - 1;
        int previousCompleteYear = latestCompleteYear - 1;
        long latestCompleteYearPaperCount = paperRepository.countByYear(latestCompleteYear);
        long previousCompleteYearPaperCount = paperRepository.countByYear(previousCompleteYear);
        int safeHighImpactCitationThreshold = Math.max(0, highImpactCitationThreshold);

        return new DashboardAnalyticsResponse(
                totalCitations,
                calculateAverage(totalCitations, totalPapers),
                paperRepository.countByCitationCountGreaterThanEqual(safeHighImpactCitationThreshold),
                latestCompleteYear,
                latestCompleteYearPaperCount,
                previousCompleteYear,
                previousCompleteYearPaperCount,
                calculateGrowthRate(latestCompleteYearPaperCount, previousCompleteYearPaperCount),
                trendService.getTopTrendingKeywords(null, 5),
                trendService.getTopTrendingTopics(null, 5));
    }

    /** Trả về số liệu vận hành dành cho Admin. */
    @Transactional(readOnly = true)
    public DashboardOperationsResponse getOperations() {
        return new DashboardOperationsResponse(
                paperRepository.countBySourceApi(SOURCE_OPENALEX),
                syncLogRepository.countByStatus(Status.SUCCESS),
                syncLogRepository.countByStatus(Status.FAILED),
                syncLogRepository.findTopByOrderByStartedAtDesc()
                        .map(SyncLogResponse::from)
                        .orElse(null));
    }

    private List<DashboardChartItemResponse> toChartItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new DashboardChartItemResponse(
                        row[0] == null ? "Unknown" : row[0].toString(),
                        ((Number) row[1]).longValue()))
                .toList();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double calculateAverage(long total, long count) {
        if (count == 0) {
            return 0.0;
        }

        return roundToTwoDecimals((double) total / count);
    }

    private double calculateGrowthRate(long currentCount, long previousCount) {
        if (previousCount == 0) {
            return currentCount > 0 ? 1.0 : 0.0;
        }

        return roundToTwoDecimals((double) (currentCount - previousCount) / previousCount);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
