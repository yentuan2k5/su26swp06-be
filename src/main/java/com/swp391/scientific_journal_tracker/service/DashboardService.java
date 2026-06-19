package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.dto.response.DashboardChartItemResponse;
import com.swp391.scientific_journal_tracker.dto.response.DashboardSummaryResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String SOURCE_OPENALEX = "openalex";
    private static final int TOP_CHART_LIMIT = 10;

    private final ResearchPaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;
    private final SyncLogRepository syncLogRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(
                paperRepository.count(),
                journalRepository.count(),
                keywordRepository.count(),
                paperRepository.countBySourceApi(SOURCE_OPENALEX),
                syncLogRepository.countByStatus(Status.SUCCESS),
                syncLogRepository.countByStatus(Status.FAILED),
                toChartItems(paperRepository.countPapersByYear()),
                toChartItems(paperRepository.countTopKeywords(PageRequest.of(0, TOP_CHART_LIMIT))),
                toChartItems(paperRepository.countTopJournals(PageRequest.of(0, TOP_CHART_LIMIT))),
                paperRepository.findTop10ByOrderByCitationCountDesc()
                        .stream()
                        .map(PaperResponse::fromEntity)
                        .toList(),
                syncLogRepository.findTopByOrderByStartedAtDesc()
                        .map(SyncLogResponse::from)
                        .orElse(null)
        );
    }

    private List<DashboardChartItemResponse> toChartItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new DashboardChartItemResponse(
                        row[0] == null ? "Unknown" : row[0].toString(),
                        ((Number) row[1]).longValue()))
                .toList();
    }
}
