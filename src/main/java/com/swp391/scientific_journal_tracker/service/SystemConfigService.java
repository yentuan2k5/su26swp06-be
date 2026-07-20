package com.swp391.scientific_journal_tracker.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.SystemConfigResponse;
import com.swp391.scientific_journal_tracker.dto.response.SystemConfigResponse.ApiDataSourceConfig;
import com.swp391.scientific_journal_tracker.dto.response.SystemConfigResponse.OpenAlexConfig;
import com.swp391.scientific_journal_tracker.dto.response.SystemConfigResponse.TrendConfig;
import com.swp391.scientific_journal_tracker.entity.ApiDataSource;
import com.swp391.scientific_journal_tracker.repository.ApiDataSourceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final ApiDataSourceRepository apiDataSourceRepository;

    @Value("${openalex.api.key:}")
    private String openAlexApiKey;

    @Value("${openalex.sync.field-ids:17}")
    private String openAlexFieldIds;

    @Value("${openalex.sync.limit:100}")
    private int openAlexSyncLimit;

    @Value("${openalex.sync.overlap-days:1}")
    private int openAlexOverlapDays;

    @Value("${openalex.backfill.max-results-per-concept:5000}")
    private int openAlexBackfillMaxResultsPerConcept;

    @Value("${trend.min-papers-threshold:30}")
    private int trendMinPapersThreshold;

    @Value("${trend.min-previous-papers-threshold:5}")
    private int trendMinPreviousPapersThreshold;

    @Value("${trend.min-recent-papers-for-emerging:30}")
    private int trendMinRecentPapersForEmerging;

    @Value("${trend.excluded-keywords:computer science}")
    private String trendExcludedKeywords;

    /**
     * Trả về cấu hình hệ thống mà admin cần quan sát.
     *
     * API key chỉ trả về trạng thái đã cấu hình hay chưa, không trả giá trị thật
     * để tránh lộ secret qua response.
     */
    @Transactional(readOnly = true)
    public SystemConfigResponse getSystemConfig() {
        return new SystemConfigResponse(
                new OpenAlexConfig(
                        parseFieldIds(openAlexFieldIds),
                        openAlexSyncLimit,
                        openAlexOverlapDays,
                        openAlexBackfillMaxResultsPerConcept,
                        openAlexApiKey != null && !openAlexApiKey.isBlank()),
                new TrendConfig(
                        trendMinPapersThreshold,
                        trendMinPreviousPapersThreshold,
                        trendMinRecentPapersForEmerging,
                        parseCommaSeparatedValues(trendExcludedKeywords)),
                apiDataSourceRepository.findAll()
                        .stream()
                        .map(this::toApiDataSourceConfig)
                        .toList());
    }

    private List<String> parseFieldIds(String fieldIds) {
        if (fieldIds == null || fieldIds.isBlank()) {
            return List.of();
        }

        return Arrays.stream(fieldIds.split(","))
                .map(String::trim)
                .map(this::normalizeOpenAlexFieldId)
                .filter(fieldId -> !fieldId.isBlank())
                .distinct()
                .toList();
    }

    private List<String> parseCommaSeparatedValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeOpenAlexFieldId(String fieldId) {
        return fieldId
                .replace("https://openalex.org/fields/", "")
                .replace("http://openalex.org/fields/", "")
                .replace("fields/", "");
    }

    private ApiDataSourceConfig toApiDataSourceConfig(ApiDataSource source) {
        return new ApiDataSourceConfig(
                source.getApiDataSourceId(),
                source.getName(),
                source.getBaseUrl(),
                source.getLastSyncTime());
    }
}
