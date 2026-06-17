package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.entity.Journal;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.SyncLog;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncService {

    private static final String SOURCE_OPENALEX = "openalex";

    private final OpenAlexClient openAlexClient;
    private final SyncLogRepository syncLogRepository;
    private final ResearchPaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;

    @Value("${openalex.sync.queries:computer science}")
    private String openAlexQueries;

    @Value("${openalex.sync.limit:10}")
    private int openAlexLimit;

    /**
     * Entry point chính, gọi từ Scheduler hoặc AdminController.
     */
    @Transactional
    public SyncLogResponse syncFromOpenAlex() {
        log.info("=== Bắt đầu sync từ OpenAlex ===");

        SyncLog syncLog = new SyncLog();
        syncLog.setSourceApi(SOURCE_OPENALEX);
        syncLog.setStatus(Status.RUNNING);
        syncLog.setStartedAt(LocalDateTime.now());
        syncLog = syncLogRepository.save(syncLog);

        int totalSynced = 0;
        int totalFailed = 0;

        try {
            List<String> queries = getConfiguredQueries();
            int limit = getConfiguredLimit();
            log.info("OpenAlex sync config: queries={}, limit={}", queries, limit);

            for (String query : queries) {
                log.info("Đang fetch papers từ OpenAlex cho query: '{}'", query);

                List<Map<String, Object>> papers = openAlexClient.searchWorks(query, limit);

                if (papers.isEmpty()) {
                    log.warn("OpenAlex không trả paper nào cho query: '{}'", query);
                }

                for (Map<String, Object> paperData : papers) {
                    try {
                        boolean saved = processOpenAlexWork(paperData);
                        if (saved) {
                            totalSynced++;
                        }
                    } catch (Exception e) {
                        totalFailed++;
                        log.warn("Bỏ qua paper do lỗi. source={}, externalId={}, title={}, error={}",
                                SOURCE_OPENALEX,
                                getString(paperData, "id"),
                                getPaperLogTitle(paperData),
                                e.getMessage(),
                                e);
                    }
                }
            }

            syncLog.setStatus(totalSynced == 0 && totalFailed > 0 ? Status.FAILED : Status.SUCCESS);
            syncLog.setPaperSynced(totalSynced);
            if (totalFailed > 0) {
                syncLog.setErrorMessage("Có " + totalFailed + " paper bị bỏ qua do lỗi. Xem backend log để biết chi tiết.");
            }
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLog = syncLogRepository.save(syncLog);

            log.info("=== Sync hoàn tất từ OpenAlex: {} papers mới, {} papers lỗi ===",
                    totalSynced, totalFailed);

        } catch (Exception e) {
            syncLog.setStatus(Status.FAILED);
            syncLog.setPaperSynced(totalSynced);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setFinishedAt(LocalDateTime.now());
            syncLog = syncLogRepository.save(syncLog);

            log.error("Sync từ OpenAlex thất bại: {}", e.getMessage(), e);
        }

        return SyncLogResponse.from(syncLog);
    }

    private List<String> getConfiguredQueries() {
        List<String> queries = Arrays.stream((openAlexQueries == null ? "" : openAlexQueries).split(","))
                .map(String::trim)
                .filter(query -> !query.isBlank())
                .toList();

        if (queries.isEmpty()) {
            return List.of("computer science");
        }

        return queries;
    }

    private int getConfiguredLimit() {
        return Math.max(1, Math.min(openAlexLimit, 100));
    }

    private boolean processOpenAlexWork(Map<String, Object> workData) {
        String externalId = getString(workData, "id");
        if (externalId == null) {
            return false;
        }

        if (paperRepository.existsByExternalId(externalId)) {
            return false;
        }

        ResearchPaper paper = new ResearchPaper();
        paper.setExternalId(externalId);
        paper.setTitle(truncate(firstNonBlank(
                getString(workData, "display_name"),
                getString(workData, "title"),
                "Untitled"), 500));
        paper.setAbstractText(restoreOpenAlexAbstract(workData.get("abstract_inverted_index")));
        paper.setSourceApi(SOURCE_OPENALEX);
        paper.setDoi(truncate(normalizeDoi(getString(workData, "doi")), 200));

        Object yearObj = workData.get("publication_year");
        if (yearObj instanceof Number year) {
            paper.setYear(year.intValue());
        }

        Object citationObj = workData.get("cited_by_count");
        if (citationObj instanceof Number citationCount) {
            paper.setCitationCount(citationCount.intValue());
        }

        paper.setAuthors(truncate(extractOpenAlexAuthors(workData), 1000));

        Map<?, ?> primaryLocation = asMap(workData.get("primary_location"));
        Map<?, ?> source = primaryLocation == null ? null : asMap(primaryLocation.get("source"));
        if (source != null) {
            Journal journal = upsertOpenAlexJournal(source, workData);
            paper.setJournalId(journal.getJournalId());
        }

        List<Keyword> keywords = extractOpenAlexKeywordTerms(workData).stream()
                .map(this::upsertKeyword)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        paper.setKeywords(keywords);

        paperRepository.save(paper);
        return true;
    }

    private Journal upsertOpenAlexJournal(Map<?, ?> source, Map<String, Object> workData) {
        String issn = firstNonBlank(getString(source, "issn_l"), firstIssn(source.get("issn")));
        String name = firstNonBlank(getString(source, "display_name"), "Unknown Journal");
        String publisher = firstNonBlank(getString(source, "host_organization_name"), getString(source, "publisher"));
        String field = firstNonBlank(extractOpenAlexField(workData), "Unknown");
        return upsertJournal(name, issn, publisher, field);
    }

    private Journal upsertJournal(String name, String issn, String publisher, String field) {
        String safeName = truncate(firstNonBlank(name, "Unknown Journal"), 255);
        String safeIssn = normalizeIssn(issn);

        if (safeIssn != null) {
            return journalRepository.findByIssn(safeIssn)
                    .orElseGet(() -> createJournal(safeName, safeIssn, publisher, field));
        }

        return journalRepository.findByTitle(safeName)
                .orElseGet(() -> createJournal(safeName, null, publisher, field));
    }

    private Journal createJournal(String name, String issn, String publisher, String field) {
        Journal journal = new Journal();
        journal.setTitle(name);
        journal.setIssn(issn != null ? issn : generateUnknownIssn(name));
        if (publisher != null) {
            journal.setPublisher(truncate(publisher, 150));
        }
        journal.setField(truncate(firstNonBlank(field, "Unknown"), 100));
        return journalRepository.save(journal);
    }

    private Keyword upsertKeyword(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }

        String safeTerm = truncate(term.trim(), 100);
        return keywordRepository.findByTerm(safeTerm)
                .orElseGet(() -> {
                    Keyword kw = new Keyword();
                    kw.setTerm(safeTerm);
                    return keywordRepository.save(kw);
                });
    }

    private String restoreOpenAlexAbstract(Object abstractObj) {
        Map<?, ?> invertedIndex = asMap(abstractObj);
        if (invertedIndex == null || invertedIndex.isEmpty()) {
            return null;
        }

        TreeMap<Integer, String> wordsByPosition = new TreeMap<>();

        for (Map.Entry<?, ?> entry : invertedIndex.entrySet()) {
            String word = entry.getKey() == null ? null : entry.getKey().toString();
            if (word == null || word.isBlank()) {
                continue;
            }

            Object positionsObj = entry.getValue();
            if (positionsObj instanceof List<?> positions) {
                for (Object positionObj : positions) {
                    if (positionObj instanceof Number position) {
                        wordsByPosition.put(position.intValue(), word);
                    }
                }
            }
        }

        if (wordsByPosition.isEmpty()) {
            return null;
        }

        return String.join(" ", wordsByPosition.values());
    }

    private String extractOpenAlexAuthors(Map<String, Object> workData) {
        Object authorshipsObj = workData.get("authorships");
        if (!(authorshipsObj instanceof List<?> authorships)) {
            return null;
        }

        return authorships.stream()
                .map(this::asMap)
                .filter(Objects::nonNull)
                .map(authorship -> asMap(authorship.get("author")))
                .filter(Objects::nonNull)
                .map(author -> getString(author, "display_name"))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    private List<String> extractOpenAlexKeywordTerms(Map<String, Object> workData) {
        List<String> terms = new ArrayList<>();
        addDisplayNames(terms, workData.get("keywords"));
        addDisplayNames(terms, workData.get("topics"));

        return terms.stream()
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .distinct()
                .limit(10)
                .toList();
    }

    private void addDisplayNames(List<String> terms, Object itemsObj) {
        if (!(itemsObj instanceof List<?> items)) {
            return;
        }

        for (Object itemObj : items) {
            Map<?, ?> item = asMap(itemObj);
            String displayName = item == null ? null : getString(item, "display_name");
            if (displayName != null) {
                terms.add(displayName);
            }
        }
    }

    private String extractOpenAlexField(Map<String, Object> workData) {
        Map<?, ?> primaryTopic = asMap(workData.get("primary_topic"));
        String primaryField = extractFieldFromTopic(primaryTopic);
        if (primaryField != null) {
            return primaryField;
        }

        Object topicsObj = workData.get("topics");
        if (topicsObj instanceof List<?> topics) {
            for (Object topicObj : topics) {
                String field = extractFieldFromTopic(asMap(topicObj));
                if (field != null) {
                    return field;
                }
            }
        }

        return null;
    }

    private String extractFieldFromTopic(Map<?, ?> topic) {
        if (topic == null) {
            return null;
        }

        Map<?, ?> field = asMap(topic.get("field"));
        return field == null ? null : getString(field, "display_name");
    }

    private String firstIssn(Object issnsObj) {
        if (issnsObj instanceof String issn && !issn.isBlank()) {
            return issn;
        }

        if (issnsObj instanceof List<?> issns) {
            for (Object issnObj : issns) {
                if (issnObj instanceof String issn && !issn.isBlank()) {
                    return issn;
                }
            }
        }

        return null;
    }

    private String normalizeIssn(String issn) {
        if (issn == null || issn.isBlank()) {
            return null;
        }

        return truncate(issn.trim(), 20);
    }

    private String normalizeDoi(String doi) {
        if (doi == null || doi.isBlank()) {
            return null;
        }

        String normalized = doi.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.startsWith("https://doi.org/")) {
            return normalized.substring("https://doi.org/".length());
        }

        if (lower.startsWith("http://dx.doi.org/")) {
            return normalized.substring("http://dx.doi.org/".length());
        }

        return normalized;
    }

    private String generateUnknownIssn(String name) {
        return "UNKNOWN-" + Integer.toUnsignedString(name.hashCode());
    }

    private String getPaperLogTitle(Map<String, Object> paperData) {
        return firstNonBlank(getString(paperData, "display_name"), getString(paperData, "title"));
    }

    private String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val instanceof String s && !s.isBlank() ? s : null;
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
