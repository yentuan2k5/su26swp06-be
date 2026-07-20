package com.swp391.scientific_journal_tracker.service;

import com.swp391.scientific_journal_tracker.dto.response.SyncLogResponse;
import com.swp391.scientific_journal_tracker.entity.Journal;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;
import com.swp391.scientific_journal_tracker.entity.SyncLog;
import com.swp391.scientific_journal_tracker.entity.SyncLog.Status;
import com.swp391.scientific_journal_tracker.repository.JournalRepository;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;
import com.swp391.scientific_journal_tracker.repository.SyncLogRepository;
import com.swp391.scientific_journal_tracker.entity.ApiDataSource;
import com.swp391.scientific_journal_tracker.entity.Author;
import com.swp391.scientific_journal_tracker.repository.ApiDataSourceRepository;
import com.swp391.scientific_journal_tracker.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncService {

    private static final String SOURCE_OPENALEX = "openalex";
    private static final String SOURCE_OPENALEX_BACKFILL = "openalex-backfill";
    private static final String OPENALEX_SOURCE_NAME = "OpenAlex";
    private static final String OPENALEX_BASE_URL = "https://api.openalex.org";

    private final OpenAlexClient openAlexClient;
    private final SyncLogRepository syncLogRepository;
    private final ResearchPaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;
    private final ResearchTopicRepository researchTopicRepository;
    private final NotificationService notificationService;
    private final AuthorRepository authorRepository;
    private final ApiDataSourceRepository apiDataSourceRepository;
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    @Value("${openalex.sync.field-ids:17}")
    private String openAlexFieldIds;

    @Value("${openalex.sync.limit:10}")
    private int openAlexLimit;

    @Value("${openalex.sync.overlap-days:1}")
    private int openAlexOverlapDays;

    @Value("${openalex.backfill.max-results-per-concept:5000}")
    private int maxResultsPerConcept;

    /**
     * Entry point chính, gọi từ Scheduler hoặc AdminController.
     */
    public SyncLogResponse syncFromOpenAlex() {
        return runExclusive("sync", this::doSyncFromOpenAlex);
    }

    private SyncLogResponse runExclusive(
            String operationName,
            Supplier<SyncLogResponse> task) {
        if (!syncInProgress.compareAndSet(false, true)) {
            log.warn(
                    "Từ chối {} vì đang có một tác vụ sync/backfill khác chạy.",
                    operationName);

            throw new IllegalStateException(
                    "Đang có một lần sync hoặc backfill khác chạy, vui lòng đợi.");
        }

        try {
            return task.get();
        } finally {
            syncInProgress.set(false);
        }
    }

    private SyncLogResponse doSyncFromOpenAlex() {
        /*
         * Ghi lại thời điểm bắt đầu trước khi gọi OpenAlex.
         *
         * Sau khi sync thành công, LastSyncTime được cập nhật
         * bằng thời điểm này thay vì thời điểm kết thúc.
         *
         * Nhờ vậy, dữ liệu phát sinh trong lúc sync đang chạy
         * vẫn được kiểm tra lại ở lần sync sau.
         */
        LocalDateTime syncStartedAt = LocalDateTime.now();

        log.info("=== Bắt đầu sync từ OpenAlex ===");

        SyncLog syncLog = new SyncLog();
        syncLog.setSourceApi(SOURCE_OPENALEX);
        syncLog.setStatus(Status.RUNNING);
        syncLog.setStartedAt(syncStartedAt);
        syncLog.setPaperSynced(0);
        syncLog = syncLogRepository.save(syncLog);

        int totalSynced = 0;
        int totalFailed = 0;

        try {
            ApiDataSource openAlexSource = getOrCreateOpenAlexDataSource();

            /*
             * Lần đầu fromCreatedDate là null.
             * Những lần sau lấy từ LastSyncTime và lùi thêm overlapDays.
             */
            LocalDate fromCreatedDate = resolveFromCreatedDate(openAlexSource);

            List<String> fieldIds = getConfiguredFieldIds();
            int limit = getConfiguredLimit();

            log.info(
                    "OpenAlex sync config: fieldIds={}, limit={}, "
                            + "lastSyncTime={}, fromCreatedDate={}",
                    fieldIds,
                    limit,
                    openAlexSource.getLastSyncTime(),
                    fromCreatedDate);

            for (String fieldId : fieldIds) {
                String filter = buildIncrementalFieldFilter(
                        fieldId,
                        fromCreatedDate);

                log.info(
                        "Đang fetch paper từ OpenAlex. "
                                + "fieldId={}, filter={}",
                        fieldId,
                        filter);

                List<Map<String, Object>> papers = openAlexClient.fetchWorksByFilter(
                        filter,
                        limit);

                if (papers.isEmpty()) {
                    log.warn(
                            "OpenAlex không trả paper nào. fieldId={}, filter={}",
                            fieldId,
                            filter);
                }

                for (Map<String, Object> paperData : papers) {
                    try {
                        boolean isNewPaper = processOpenAlexWork(
                                paperData,
                                openAlexSource,
                                true);

                        if (isNewPaper) {
                            totalSynced++;
                        }

                    } catch (Exception exception) {
                        totalFailed++;

                        log.warn(
                                "Bỏ qua paper do lỗi. "
                                        + "source={}, externalId={}, "
                                        + "title={}, error={}",
                                SOURCE_OPENALEX,
                                getString(paperData, "id"),
                                getPaperLogTitle(paperData),
                                exception.getMessage(),
                                exception);
                    }
                }
            }

            /*
             * Chỉ tiến mốc đồng bộ khi không có paper nào bị lỗi.
             * Nếu có lỗi, giữ nguyên mốc cũ để lần sau thử lại.
             */
            if (totalFailed == 0) {
                openAlexSource.setLastSyncTime(syncStartedAt);
                apiDataSourceRepository.save(openAlexSource);
            } else {
                log.warn(
                        "Không cập nhật LastSyncTime "
                                + "vì có {} paper bị lỗi",
                        totalFailed);
            }

            syncLog.setStatus(
                    totalSynced == 0 && totalFailed > 0
                            ? Status.FAILED
                            : Status.SUCCESS);

            syncLog.setPaperSynced(totalSynced);

            if (totalFailed > 0) {
                syncLog.setErrorMessage(
                        "Có " + totalFailed
                                + " paper bị bỏ qua do lỗi. "
                                + "Xem backend log để biết chi tiết.");
            } else {
                syncLog.setErrorMessage(null);
            }

            syncLog.setFinishedAt(LocalDateTime.now());
            SyncLog savedLog = syncLogRepository.save(syncLog);

            log.info(
                    "=== Sync OpenAlex hoàn tất: "
                            + "{} paper mới, {} paper lỗi ===",
                    totalSynced,
                    totalFailed);

            // Return cho trường hợp sync chạy xong bình thường.
            return SyncLogResponse.from(savedLog);

        } catch (Exception exception) {
            log.error(
                    "Sync OpenAlex thất bại. Root cause: {}",
                    exception.getMessage(),
                    exception);

            syncLog.setStatus(Status.FAILED);

            // File của bạn dùng totalSynced, không có biến paperSynced.
            syncLog.setPaperSynced(totalSynced);

            syncLog.setErrorMessage(
                    exception.getMessage() == null
                            ? "Không xác định được lỗi sync"
                            : exception.getMessage());

            syncLog.setFinishedAt(LocalDateTime.now());

            SyncLog savedLog = syncLogRepository.save(syncLog);

            return SyncLogResponse.from(savedLog);
        }
    }

    /**
     * Backfill dữ liệu lịch sử từ OpenAlex theo field và năm xuất bản.
     *
     * Luồng này không cập nhật ApiDataSource.lastSyncTime vì mốc đó
     * thuộc riêng incremental sync.
     */
    public SyncLogResponse backfillFromOpenAlex(
            int fromYear,
            int toYear,
            List<String> fieldIds) {
        return backfillFromOpenAlex(
                fromYear,
                toYear,
                fieldIds,
                null);
    }

    public SyncLogResponse backfillFromOpenAlex(
            int fromYear,
            int toYear,
            List<String> fieldIds,
            Integer maxResultsOverride) {
        return runExclusive(
                "backfill",
                () -> doBackfillFromOpenAlex(
                        fromYear,
                        toYear,
                        fieldIds,
                        maxResultsOverride));
    }

    private SyncLogResponse doBackfillFromOpenAlex(
            int fromYear,
            int toYear,
            List<String> fieldIds,
            Integer maxResultsOverride) {
        if (fromYear > toYear) {
            throw new IllegalArgumentException(
                    "fromYear không được lớn hơn toYear");
        }

        List<String> safeFieldIds = normalizeOpenAlexFieldIds(fieldIds);

        if (safeFieldIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Danh sách fieldIds không được để trống");
        }

        int safeMaxResultsPerConcept = resolveBackfillMaxResults(maxResultsOverride);

        LocalDateTime backfillStartedAt = LocalDateTime.now();

        log.info(
                "=== Bắt đầu backfill OpenAlex: fromYear={}, "
                        + "toYear={}, fieldIds={}, maxResultsPerConcept={} ===",
                fromYear,
                toYear,
                safeFieldIds,
                safeMaxResultsPerConcept);

        SyncLog syncLog = new SyncLog();
        syncLog.setSourceApi(SOURCE_OPENALEX_BACKFILL);
        syncLog.setStatus(Status.RUNNING);
        syncLog.setStartedAt(backfillStartedAt);
        syncLog.setPaperSynced(0);
        syncLog = syncLogRepository.save(syncLog);

        int totalSynced = 0;
        int totalFailed = 0;

        try {
            ApiDataSource openAlexSource = getOrCreateOpenAlexDataSource();

            for (String fieldId : safeFieldIds) {
                String filter = buildBackfillFilter(
                        fieldId,
                        fromYear,
                        toYear);

                log.info(
                        "Đang backfill OpenAlex. fieldId={}, filter={}",
                        fieldId,
                        filter);

                List<Map<String, Object>> papers = openAlexClient.fetchWorksByFilter(
                        filter,
                        safeMaxResultsPerConcept);

                if (papers.isEmpty()) {
                    log.warn(
                            "OpenAlex không trả paper nào khi backfill. "
                                    + "fieldId={}, filter={}",
                            fieldId,
                            filter);
                }

                for (Map<String, Object> paperData : papers) {
                    try {
                        boolean isNewPaper = processOpenAlexWork(
                                paperData,
                                openAlexSource,
                                false);

                        if (isNewPaper) {
                            totalSynced++;
                        }

                    } catch (Exception exception) {
                        totalFailed++;

                        log.warn(
                                "Bỏ qua paper backfill do lỗi. "
                                        + "source={}, fieldId={}, externalId={}, "
                                        + "title={}, error={}",
                                SOURCE_OPENALEX_BACKFILL,
                                fieldId,
                                getString(paperData, "id"),
                                getPaperLogTitle(paperData),
                                exception.getMessage(),
                                exception);
                    }
                }
            }

            syncLog.setStatus(
                    totalSynced == 0 && totalFailed > 0
                            ? Status.FAILED
                            : Status.SUCCESS);

            syncLog.setPaperSynced(totalSynced);

            if (totalFailed > 0) {
                syncLog.setErrorMessage(
                        "Có " + totalFailed
                                + " paper backfill bị bỏ qua do lỗi. "
                                + "Xem backend log để biết chi tiết.");
            } else {
                syncLog.setErrorMessage(null);
            }

            syncLog.setFinishedAt(LocalDateTime.now());
            SyncLog savedLog = syncLogRepository.save(syncLog);

            log.info(
                    "=== Backfill OpenAlex hoàn tất: "
                            + "{} paper mới, {} paper lỗi ===",
                    totalSynced,
                    totalFailed);

            return SyncLogResponse.from(savedLog);

        } catch (Exception exception) {
            log.error(
                    "Backfill OpenAlex thất bại. Root cause: {}",
                    exception.getMessage(),
                    exception);

            syncLog.setStatus(Status.FAILED);
            syncLog.setPaperSynced(totalSynced);
            syncLog.setErrorMessage(
                    exception.getMessage() == null
                            ? "Không xác định được lỗi backfill"
                            : exception.getMessage());
            syncLog.setFinishedAt(LocalDateTime.now());

            SyncLog savedLog = syncLogRepository.save(syncLog);

            return SyncLogResponse.from(savedLog);
        }
    }

    private int resolveBackfillMaxResults(Integer maxResultsOverride) {
        int configuredMaxResults = Math.max(
                1,
                maxResultsPerConcept);

        if (maxResultsOverride == null) {
            return configuredMaxResults;
        }

        return Math.max(
                1,
                Math.min(maxResultsOverride, configuredMaxResults));
    }

    private List<String> normalizeOpenAlexFieldIds(List<String> fieldIds) {
        if (fieldIds == null) {
            return List.of();
        }

        return fieldIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(this::normalizeOpenAlexFieldId)
                .filter(fieldId -> !fieldId.isBlank())
                .distinct()
                .toList();
    }

    private String buildBackfillFilter(
            String fieldId,
            int fromYear,
            int toYear) {
        /*
         * OpenAlex filter không dùng search free-text.
         * Computer Science là field id 17 trong topic hierarchy.
         *
         * Dùng primary_topic.field.id để lấy paper có lĩnh vực chính
         * thuộc field đó, thay vì tìm chữ "computer science" trong text.
         *
         * Với range năm, dùng 2 filter inequality để tránh cú pháp
         * publication_year:2021-2026 không tương thích.
         */
        if (fromYear == toYear) {
            return "primary_topic.field.id:" + fieldId
                    + ",publication_year:" + fromYear;
        }

        return "primary_topic.field.id:" + fieldId
                + ",publication_year:>" + (fromYear - 1)
                + ",publication_year:<" + (toYear + 1);
    }

    private List<String> getConfiguredFieldIds() {
        List<String> fieldIds = Arrays.stream((openAlexFieldIds == null ? "" : openAlexFieldIds).split(","))
                .map(String::trim)
                .map(this::normalizeOpenAlexFieldId)
                .filter(fieldId -> !fieldId.isBlank())
                .distinct()
                .toList();

        if (fieldIds.isEmpty()) {
            return List.of("17");
        }

        return fieldIds;
    }

    private String normalizeOpenAlexFieldId(String fieldId) {
        if (fieldId == null || fieldId.isBlank()) {
            return "";
        }

        return fieldId.trim()
                .replace("https://openalex.org/fields/", "")
                .replace("http://openalex.org/fields/", "")
                .replace("fields/", "");
    }

    private String buildIncrementalFieldFilter(
            String fieldId,
            LocalDate fromCreatedDate) {
        String filter = "primary_topic.field.id:" + fieldId;

        if (fromCreatedDate != null) {
            filter += ",from_created_date:" + fromCreatedDate;
        }

        return filter;
    }

    private int getConfiguredLimit() {
        return Math.max(1, Math.min(openAlexLimit, 100));
    }

    /**
     * Xác định ngày OpenAlex tạo record bắt đầu cần lấy.
     *
     * Lần đầu:
     * - LastSyncTime chưa có.
     * - Trả về null để lấy các record mới nhất.
     *
     * Những lần sau:
     * - Lấy ngày của LastSyncTime.
     * - Lùi lại overlapDays để hạn chế bỏ sót dữ liệu.
     */
    private LocalDate resolveFromCreatedDate(
            ApiDataSource openAlexSource) {
        if (openAlexSource.getLastSyncTime() == null) {
            log.info(
                    "OpenAlex chưa từng sync. "
                            + "Lần đầu lấy các record mới nhất.");

            return null;
        }

        int safeOverlapDays = Math.max(openAlexOverlapDays, 0);

        return openAlexSource
                .getLastSyncTime()
                .toLocalDate()
                .minusDays(safeOverlapDays);
    }

    private ApiDataSource getOrCreateOpenAlexDataSource() {
        return apiDataSourceRepository
                .findByNameIgnoreCase(OPENALEX_SOURCE_NAME)
                .orElseGet(() -> {
                    ApiDataSource source = new ApiDataSource();
                    source.setName(OPENALEX_SOURCE_NAME);
                    source.setBaseUrl(OPENALEX_BASE_URL);
                    source.setLastSyncTime(null);

                    return apiDataSourceRepository.save(source);
                });
    }

    private boolean processOpenAlexWork(
            Map<String, Object> workData,
            ApiDataSource apiDataSource,
            boolean sendNotification) {
        String externalId = getString(workData, "id");
        if (externalId == null) {
            return false;
        }

        List<ResearchPaper> existingPapers = paperRepository.findByExternalId(externalId);
        boolean isNewPaper = existingPapers.isEmpty();

        ResearchPaper paper = isNewPaper ? new ResearchPaper() : existingPapers.get(0);

        paper.setExternalId(externalId);
        paper.setTitle(truncate(firstNonBlank(
                getString(workData, "display_name"),
                getString(workData, "title"),
                "Untitled"), 500));
        paper.setAbstractText(
                restoreOpenAlexAbstract(workData.get("abstract_inverted_index")));

        paper.setApiDataSource(apiDataSource);
        paper.setSourceApi(SOURCE_OPENALEX);

        paper.setDoi(truncate(
                normalizeDoi(getString(workData, "doi")), 200));
        Object yearObj = workData.get("publication_year");
        if (yearObj instanceof Number year) {
            paper.setYear(year.intValue());
        }

        Object citationObj = workData.get("cited_by_count");
        if (citationObj instanceof Number citationCount) {
            paper.setCitationCount(citationCount.intValue());
        }

        // Giữ chuỗi tên tác giả cũ để hiển thị nhanh và tương thích code cũ
        paper.setAuthorsRaw(
                truncate(extractOpenAlexAuthors(workData), 1000));

        // Lưu quan hệ chuẩn hóa ResearchPaper - Author
        paper.setAuthors(
                extractAndUpsertAuthors(workData));

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

        List<ResearchTopic> topics = extractOpenAlexTopicNames(workData).stream()
                .map(this::upsertTopic)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        paper.setResearchTopics(topics);

        ResearchPaper savedPaper = paperRepository.save(paper);

        if (isNewPaper && sendNotification) {
            notificationService.createNewPaperNotifications(savedPaper.getResearchPaperId());
        }

        return isNewPaper;
    }

    private List<Author> extractAndUpsertAuthors(
            Map<String, Object> workData) {
        Object authorshipsObj = workData.get("authorships");

        if (!(authorshipsObj instanceof List<?> authorships)) {
            return List.of();
        }

        return authorships.stream()
                .map(this::asMap)
                .filter(Objects::nonNull)
                .map(authorship -> asMap(authorship.get("author")))
                .filter(Objects::nonNull)
                .map(authorData -> upsertAuthor(
                        getString(authorData, "id"),
                        getString(authorData, "display_name")))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Author upsertAuthor(
            String externalId,
            String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }

        String safeName = truncate(fullName.trim(), 255);

        String safeExternalId = externalId == null || externalId.isBlank()
                ? null
                : truncate(externalId.trim(), 100);

        /*
         * OpenAlex author ID là phương thức dedupe chính xác nhất.
         */
        if (safeExternalId != null) {
            return authorRepository.findByExternalId(safeExternalId)
                    .map(existingAuthor -> {
                        // Cập nhật tên nếu OpenAlex thay đổi display_name
                        if (!safeName.equals(existingAuthor.getFullName())) {
                            existingAuthor.setFullName(safeName);
                            return authorRepository.save(existingAuthor);
                        }

                        return existingAuthor;
                    })
                    .orElseGet(() -> {
                        Author author = new Author();
                        author.setExternalId(safeExternalId);
                        author.setFullName(safeName);

                        return authorRepository.save(author);
                    });
        }

        /*
         * Nếu không có OpenAlex ID thì tạm dedupe theo tên.
         */
        return authorRepository
                .findFirstByFullNameIgnoreCase(safeName)
                .orElseGet(() -> {
                    Author author = new Author();
                    author.setFullName(safeName);

                    return authorRepository.save(author);
                });
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

    private ResearchTopic upsertTopic(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String safeName = truncate(name.trim(), 150);

        return researchTopicRepository.findByNameIgnoreCase(safeName)
                .orElseGet(() -> {
                    ResearchTopic topic = new ResearchTopic();
                    topic.setName(safeName);
                    topic.setDescription("Imported from OpenAlex");
                    return researchTopicRepository.save(topic);
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

        return terms.stream()
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .distinct()
                .limit(10)
                .toList();
    }

    private List<String> extractOpenAlexTopicNames(Map<String, Object> workData) {
        List<String> topics = new ArrayList<>();

        Map<?, ?> primaryTopic = asMap(workData.get("primary_topic"));
        if (primaryTopic != null) {
            String primaryTopicName = getString(primaryTopic, "display_name");
            if (primaryTopicName != null) {
                topics.add(primaryTopicName);
            }
        }

        addDisplayNames(topics, workData.get("topics"));

        return topics.stream()
                .map(String::trim)
                .filter(topic -> !topic.isBlank())
                .distinct()
                .limit(5)
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
