package com.swp391.scientific_journal_tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
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
            "publication_year",
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
                .codecs(config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchWorks(String query, int limit) {
        if (openAlexApiKey == null || openAlexApiKey.isBlank()) {
            throw new RuntimeException("OpenAlex cần OPENALEX_API_KEY. Hãy tạo key ở https://openalex.org/settings/api");
        }

        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/works")
                            .queryParam("search", query)
                            .queryParam("per_page", limit)
                            .queryParam("select", WORK_FIELDS)
                            .queryParam("api_key", openAlexApiKey.trim())
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            if (response == null || !response.containsKey("results")) {
                log.warn("OpenAlex trả về response rỗng cho query: {}", query);
                return Collections.emptyList();
            }

            Object results = response.get("results");

            if (!(results instanceof List<?>)) {
                log.warn("OpenAlex field results không phải List, query={}", query);
                return Collections.emptyList();
            }

            List<Map<String, Object>> works = (List<Map<String, Object>>) results;
            log.info("OpenAlex query '{}' trả về {} works", query, works.size());

            return works;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.error("OpenAlex bị rate limit 429. Query={}. Body={}",
                        query, e.getResponseBodyAsString(), e);
                throw new RuntimeException("OpenAlex rate limit 429. Hãy giảm OPENALEX_SYNC_LIMIT hoặc thử lại sau.", e);
            }

            log.error("OpenAlex API lỗi HTTP {}. Query={}. Body={}",
                    e.getStatusCode(), query, e.getResponseBodyAsString(), e);

            throw new RuntimeException("OpenAlex API error: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Lỗi khi gọi OpenAlex API, query={}: {}", query, e.getMessage(), e);
            throw new RuntimeException("OpenAlex API error: " + e.getMessage(), e);
        }
    }
}
