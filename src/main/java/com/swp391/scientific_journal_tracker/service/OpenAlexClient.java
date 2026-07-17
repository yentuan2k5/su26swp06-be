package com.swp391.scientific_journal_tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAlexClient {

        private static final String BASE_URL = "https://api.openalex.org";

        private static final String WORK_FIELDS = String.join(",",
                        "id",
                        "doi",
                        "title",
                        "display_name",
                        "publication_date",
                        "publication_year",
                        "created_date",
                        "updated_date",
                        "cited_by_count",
                        "abstract_inverted_index",
                        "authorships",
                        "primary_location",
                        "primary_topic",
                        "keywords",
                        "topics");

        private final WebClient webClient;

        @Value("${openalex.api.key:}")
        private String openAlexApiKey;

        public OpenAlexClient() {
                this.webClient = WebClient.builder()
                                .baseUrl(BASE_URL)
                                .codecs(configurer -> configurer.defaultCodecs()
                                                .maxInMemorySize(5 * 1024 * 1024))
                                .build();
        }

        /**
         * Lấy các công trình mới được thêm vào OpenAlex
         * kể từ fromCreatedDate.
         *
         * fromCreatedDate = null nghĩa là lần sync đầu tiên.
         */
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> searchWorks(
                        String query,
                        int limit,
                        LocalDate fromCreatedDate) {
                if (query == null || query.isBlank()) {
                        throw new IllegalArgumentException(
                                        "OpenAlex search query không được để trống");
                }

                if (openAlexApiKey == null || openAlexApiKey.isBlank()) {
                        throw new IllegalStateException(
                                        "Thiếu OPENALEX_API_KEY. "
                                                        + "Hãy cấu hình API key trước khi đồng bộ");
                }

                /*
                 * OpenAlex cho phép per_page từ 1 đến 100.
                 */
                int safeLimit = Math.max(1, Math.min(limit, 100));

                try {
                        Map<String, Object> response = webClient.get()
                                        .uri(uriBuilder -> buildWorksUri(
                                                        uriBuilder,
                                                        query.trim(),
                                                        safeLimit,
                                                        fromCreatedDate))
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .timeout(Duration.ofSeconds(20))
                                        .block();

                        if (response == null) {
                                log.warn(
                                                "OpenAlex trả về response null. query={}",
                                                query);

                                return Collections.emptyList();
                        }

                        Object resultsObject = response.get("results");

                        if (!(resultsObject instanceof List<?>)) {
                                log.warn(
                                                "OpenAlex không trả về results dạng List. query={}",
                                                query);

                                return Collections.emptyList();
                        }

                        List<Map<String, Object>> works = (List<Map<String, Object>>) resultsObject;

                        log.info(
                                        "OpenAlex query='{}', fromCreatedDate={}, "
                                                        + "limit={}, returned={}",
                                        query,
                                        fromCreatedDate,
                                        safeLimit,
                                        works.size());

                        return works;

                } catch (WebClientResponseException exception) {
                        int statusCode = exception.getStatusCode().value();

                        if (statusCode == 429) {
                                log.error(
                                                "OpenAlex rate limit 429. query={}, body={}",
                                                query,
                                                exception.getResponseBodyAsString(),
                                                exception);

                                throw new IllegalStateException(
                                                "OpenAlex đang giới hạn request. "
                                                                + "Hãy thử lại sau",
                                                exception);
                        }

                        log.error(
                                        "OpenAlex HTTP error. status={}, query={}, body={}",
                                        statusCode,
                                        query,
                                        exception.getResponseBodyAsString(),
                                        exception);

                        throw new IllegalStateException(
                                        "OpenAlex API trả lỗi HTTP " + statusCode,
                                        exception);

                } catch (Exception exception) {
                        log.error(
                                        "Không thể gọi OpenAlex. query={}, error={}",
                                        query,
                                        exception.getMessage(),
                                        exception);

                        throw new IllegalStateException(
                                        "Không thể gọi OpenAlex API: "
                                                        + exception.getMessage(),
                                        exception);
                }
        }

        /**
         * Lấy works bằng OpenAlex filter API.
         *
         * Method này dùng cho backfill dữ liệu lịch sử theo topic / năm xuất bản,
         * tách riêng khỏi searchWorks() đang phục vụ incremental sync.
         */
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> fetchWorksByFilter(
                        String filter,
                        int maxResults) {
                if (filter == null || filter.isBlank()) {
                        throw new IllegalArgumentException(
                                        "OpenAlex filter không được để trống");
                }

                if (maxResults <= 0) {
                        return List.of();
                }

                if (openAlexApiKey == null || openAlexApiKey.isBlank()) {
                        throw new IllegalStateException(
                                        "Thiếu OPENALEX_API_KEY. "
                                                        + "Hãy cấu hình API key trước khi đồng bộ");
                }

                String safeFilter = filter.trim();
                List<Map<String, Object>> works = new ArrayList<>();
                String cursor = "*";

                while (cursor != null
                                && !cursor.isBlank()
                                && works.size() < maxResults) {

                        ResponseEntity<Map<String, Object>> responseEntity = requestWorksByFilterPage(
                                        safeFilter,
                                        cursor);

                        Map<String, Object> response = responseEntity.getBody();

                        if (response == null) {
                                log.warn(
                                                "OpenAlex trả về response null. filter={}, cursor={}",
                                                safeFilter,
                                                cursor);

                                break;
                        }

                        logOpenAlexCostAndRateLimit(
                                        safeFilter,
                                        cursor,
                                        response,
                                        responseEntity.getHeaders());

                        if (shouldStopForLowRemainingUsd(
                                        responseEntity.getHeaders())) {
                                log.warn(
                                                "Dừng fetch OpenAlex sớm vì remaining USD thấp. "
                                                                + "filter={}, fetched={}",
                                                safeFilter,
                                                works.size());

                                break;
                        }

                        Object resultsObject = response.get("results");

                        if (!(resultsObject instanceof List<?> results)) {
                                log.warn(
                                                "OpenAlex không trả về results dạng List. "
                                                                + "filter={}, cursor={}",
                                                safeFilter,
                                                cursor);

                                break;
                        }

                        int remainingSlots = maxResults - works.size();

                        results.stream()
                                        .filter(item -> item instanceof Map<?, ?>)
                                        .limit(remainingSlots)
                                        .map(item -> (Map<String, Object>) item)
                                        .forEach(works::add);

                        cursor = extractNextCursor(response);

                        log.info(
                                        "OpenAlex filter='{}', fetched={}, "
                                                        + "lastPageReturned={}, nextCursorExists={}",
                                        safeFilter,
                                        works.size(),
                                        results.size(),
                                        cursor != null && !cursor.isBlank());
                }

                return works;
        }

        private ResponseEntity<Map<String, Object>> requestWorksByFilterPage(
                        String filter,
                        String cursor) {
                int maxRetries = 3;

                for (int attempt = 0; attempt <= maxRetries; attempt++) {
                        try {
                                return webClient.get()
                                                .uri(uriBuilder -> buildWorksFilterUri(
                                                                uriBuilder,
                                                                filter,
                                                                cursor))
                                                .retrieve()
                                                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                                                })
                                                .timeout(Duration.ofSeconds(20))
                                                .block();

                        } catch (WebClientResponseException exception) {
                                int statusCode = exception.getStatusCode().value();

                                if (statusCode == 429) {
                                        if (attempt == maxRetries) {
                                                log.error(
                                                                "OpenAlex rate limit 429 sau khi retry hết. "
                                                                                + "filter={}, cursor={}, body={}",
                                                                filter,
                                                                cursor,
                                                                exception.getResponseBodyAsString(),
                                                                exception);

                                                throw new IllegalStateException(
                                                                "OpenAlex đang giới hạn request. "
                                                                                + "Hãy thử lại sau",
                                                                exception);
                                        }

                                        int retryNumber = attempt + 1;

                                        log.warn(
                                                        "OpenAlex rate limit 429. "
                                                                        + "filter={}, cursor={}, retry={}/{}, body={}",
                                                        filter,
                                                        cursor,
                                                        retryNumber,
                                                        maxRetries,
                                                        exception.getResponseBodyAsString());

                                        sleepBeforeRetry(retryNumber);
                                        continue;
                                }

                                log.error(
                                                "OpenAlex HTTP error. status={}, filter={}, cursor={}, body={}",
                                                statusCode,
                                                filter,
                                                cursor,
                                                exception.getResponseBodyAsString(),
                                                exception);

                                throw new IllegalStateException(
                                                "OpenAlex API trả lỗi HTTP " + statusCode,
                                                exception);
                        }
                }

                throw new IllegalStateException(
                                "Không thể gọi OpenAlex API sau khi retry");
        }

        private void sleepBeforeRetry(int attempt) {
                long backoffMs = (long) Math.pow(2, attempt) * 1000;

                try {
                        Thread.sleep(backoffMs);
                } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();

                        throw new IllegalStateException(
                                        "OpenAlex retry bị gián đoạn",
                                        exception);
                }
        }

        private void logOpenAlexCostAndRateLimit(
                        String filter,
                        String cursor,
                        Map<String, Object> response,
                        HttpHeaders headers) {
                String remainingUsd = firstNonBlank(
                                headers.getFirst("X-RateLimit-Remaining-USD"),
                                headers.getFirst("X-RateLimit-Remaining"));

                if (remainingUsd != null) {
                        log.info(
                                        "OpenAlex rate-limit remaining. "
                                                        + "filter={}, cursor={}, remaining={}",
                                        filter,
                                        cursor,
                                        remainingUsd);
                }

                Map<?, ?> meta = asMap(response.get("meta"));

                if (meta == null) {
                        return;
                }

                Object costUsd = meta.get("cost_usd");

                if (costUsd != null) {
                        log.info(
                                        "OpenAlex request cost. "
                                                        + "filter={}, cursor={}, costUsd={}",
                                        filter,
                                        cursor,
                                        costUsd);
                }
        }

        private boolean shouldStopForLowRemainingUsd(HttpHeaders headers) {
                String remainingUsd = headers.getFirst("X-RateLimit-Remaining-USD");

                if (remainingUsd == null || remainingUsd.isBlank()) {
                        return false;
                }

                try {
                        return Double.parseDouble(remainingUsd.trim()) < 0.05;
                } catch (NumberFormatException exception) {
                        log.warn(
                                        "Không đọc được X-RateLimit-Remaining-USD={}",
                                        remainingUsd);

                        return false;
                }
        }

        private String extractNextCursor(Map<String, Object> response) {
                Map<?, ?> meta = asMap(response.get("meta"));

                if (meta == null) {
                        return null;
                }

                Object nextCursor = meta.get("next_cursor");

                if (nextCursor instanceof String value && !value.isBlank()) {
                        return value;
                }

                return null;
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

        /**
         * Tạo URL gọi OpenAlex.
         *
         * Lần đầu:
         * - Không có filter.
         * - Lấy các record mới được OpenAlex tạo gần đây nhất.
         *
         * Những lần sau:
         * - Chỉ lấy record được OpenAlex tạo từ fromCreatedDate.
         */
        private URI buildWorksUri(
                        UriBuilder uriBuilder,
                        String query,
                        int limit,
                        LocalDate fromCreatedDate) {
                UriBuilder builder = uriBuilder
                                .path("/works")
                                .queryParam("search", query)
                                .queryParam("per_page", limit)
                                .queryParam("sort", "publication_date:desc")
                                .queryParam("select", WORK_FIELDS)
                                .queryParam("api_key", openAlexApiKey.trim());

                if (fromCreatedDate != null) {
                        builder.queryParam(
                                        "filter",
                                        "from_created_date:" + fromCreatedDate);
                }

                return builder.build();
        }

        private URI buildWorksFilterUri(
                        UriBuilder uriBuilder,
                        String filter,
                        String cursor) {
                return uriBuilder
                                .path("/works")
                                .queryParam("filter", filter)
                                .queryParam("per_page", 100)
                                .queryParam("cursor", cursor)
                                .queryParam("select", WORK_FIELDS)
                                .queryParam("api_key", openAlexApiKey.trim())
                                .build();
        }
}
