package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.entity.*;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import com.swp391.scientific_journal_tracker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncService {

    private final SemanticScholarClient semanticScholarClient;
    private final SyncLogRepository syncLogRepository;
    private final ResearchPaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;

    // Các query mặc định khi sync (có thể lấy từ config sau)
    private static final List<String> DEFAULT_QUERIES = List.of(
            "artificial intelligence",
            "machine learning",
            "deep learning",
            "computer science");
    private static final int PAPERS_PER_QUERY = 25; // Giới hạn để không bị rate limit

    /**
     * Entry point chính — gọi từ Scheduler hoặc AdminController
     * Trả về SyncLogResponse để controller có thể trả về cho client
     */
    @Transactional
    public SyncLogResponse syncFromSemanticScholar() {
        log.info("=== Bắt đầu sync từ Semantic Scholar ===");

        // 1. Tạo SyncLog với status RUNNING
        SyncLog syncLog = new SyncLog();
        syncLog.setSourceApi("semantic_scholar");
        syncLog.setStatus(Status.RUNNING);
        syncLog.setStartedAt(LocalDateTime.now());
        syncLog = syncLogRepository.save(syncLog);

        int totalSynced = 0;

        try {
            // 2. Lặp qua từng query
            for (String query : DEFAULT_QUERIES) {
                log.info("Đang fetch papers cho query: '{}'", query);

                List<Map<String, Object>> papers = semanticScholarClient.searchPapers(query, PAPERS_PER_QUERY);

                for (Map<String, Object> paperData : papers) {
                    try {
                        boolean saved = processPaper(paperData);
                        if (saved)
                            totalSynced++;
                    } catch (Exception e) {
                        // Bỏ qua paper lỗi, tiếp tục xử lý các paper khác
                        log.warn("Bỏ qua paper do lỗi: {}", e.getMessage());
                    }
                }
            }

            // 3. Cập nhật SyncLog: SUCCESS
            syncLog.setStatus(Status.SUCCESS);
            syncLog.setPaperSynced(totalSynced);
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLog = syncLogRepository.save(syncLog);

            log.info("=== Sync hoàn tất: {} papers mới ===", totalSynced);

        } catch (Exception e) {
            // 4. Cập nhật SyncLog: FAILED
            syncLog.setStatus(Status.FAILED);
            syncLog.setPaperSynced(totalSynced);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLog = syncLogRepository.save(syncLog);

            log.error("Sync thất bại: {}", e.getMessage(), e);
        }

        return SyncLogResponse.from(syncLog);
    }

    /**
     * Xử lý 1 paper từ API response.
     * Trả về true nếu paper được lưu mới, false nếu đã tồn tại (skip)
     */
    private boolean processPaper(Map<String, Object> paperData) {
        String externalId = (String) paperData.get("paperId");
        if (externalId == null || externalId.isBlank())
            return false;

        // Skip nếu paper đã tồn tại trong DB (upsert-like behavior)
        if (paperRepository.existsByExternalId(externalId)) {
            return false;
        }

        ResearchPaper paper = new ResearchPaper();
        paper.setExternalId(externalId);
        paper.setTitle(getStringOrDefault(paperData, "title", "Untitled"));
        paper.setAbstractText(getStringOrDefault(paperData, "abstract", null));
        paper.setSourceApi("semantic_scholar");

        // Year
        Object yearObj = paperData.get("year");
        if (yearObj instanceof Integer) {
            paper.setYear((Integer) yearObj);
        }

        // Citation count
        Object citObj = paperData.get("citationCount");
        if (citObj instanceof Integer) {
            paper.setCitationCount((Integer) citObj);
        }

        // Authors — gộp tên thành 1 string
        Object authorsObj = paperData.get("authors");
        if (authorsObj instanceof List<?> authorsList) {
            String authors = authorsList.stream()
                    .filter(a -> a instanceof Map)
                    .map(a -> (String) ((Map<?, ?>) a).get("name"))
                    .filter(name -> name != null)
                    .collect(Collectors.joining(", "));
            paper.setAuthors(authors);
        }

        // DOI từ externalIds
        Object externalIdsObj = paperData.get("externalIds");
        if (externalIdsObj instanceof Map<?, ?> externalIds) {
            Object doi = externalIds.get("DOI");
            if (doi instanceof String)
                paper.setDoi((String) doi);
        }

        // Journal — upsert theo ISSN hoặc tên
        Object venueObj = paperData.get("publicationVenue");
        if (venueObj instanceof Map<?, ?> venue) {
            Journal journal = upsertJournal(venue);
            // Liên kết paper với journal thông qua JournalId column
            // Note: do mapping dùng insertable=false/updatable=false nên cần set trực tiếp
            // Bạn cần thêm @Column JournalId vào ResearchPaper (xem ghi chú bên dưới)
            paper.setJournalId(journal.getJournalId());
        }

        // Keywords từ fieldsOfStudy
        Object fieldsObj = paperData.get("fieldsOfStudy");
        if (fieldsObj instanceof List<?> fields) {
            List<Keyword> keywords = fields.stream()
                    .filter(f -> f instanceof String)
                    .map(f -> upsertKeyword((String) f))
                    .collect(Collectors.toList());
            paper.setKeywords(keywords);
        }

        paperRepository.save(paper);
        return true;
    }

    /**
     * Tìm Journal theo ISSN, nếu chưa có thì tạo mới
     */

    private Journal upsertJournal(Map<?, ?> venue) {
        // Lấy ISSN từ venue data
        String issn = null;
        Object issns = venue.get("issn");
        if (issns instanceof List<?> issnList && !issnList.isEmpty()) {
            issn = (String) issnList.get(0);
        }
        String name = venue.get("name") instanceof String s ? s : "Unknown Journal";

        // Tìm theo ISSN hoặc tên
        if (issn != null) {
            final String finalIssn = issn;
            return journalRepository.findByIssn(issn)
                    .orElseGet(() -> createJournal(name, finalIssn, venue));
        } else {
            return journalRepository.findByTitle(name)
                    .orElseGet(() -> createJournal(name, null, venue));
        }
    }

    private Journal createJournal(String name, String issn, Map<?, ?> venue) {
        Journal journal = new Journal();
        journal.setTitle(name);
        journal.setIssn(issn != null ? issn : "UNKNOWN-" + System.currentTimeMillis());
        Object publisher = venue.get("publisher");
        if (publisher instanceof String)
            journal.setPublisher((String) publisher);
        journal.setField("Computer Science"); // Default field
        return journalRepository.save(journal);
    }

    /**
     * Tìm Keyword theo term, nếu chưa có thì tạo mới
     */
    private Keyword upsertKeyword(String term) {
        return keywordRepository.findByTerm(term)
                .orElseGet(() -> {
                    Keyword kw = new Keyword();
                    kw.setTerm(term);
                    return keywordRepository.save(kw);
                });
    }

    // Helper: đọc String từ Map, trả về defaultValue nếu null
    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val instanceof String s ? s : defaultValue;
    }
}
