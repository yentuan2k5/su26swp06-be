package com.swp391.scientific_journal_tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
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
}