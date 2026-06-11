package com.swp391.scientific_journal_tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SemanticScholarClient {

    // Base URL của Semantic Scholar Academic Graph API (miễn phí)
    private static final String BASE_URL = "https://api.semanticscholar.org/graph/v1";

    private final WebClient webClient;

    public SemanticScholarClient() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                // Tăng buffer size vì response có thể lớn
                .codecs(config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();
    }

    /**
     * Tìm kiếm papers theo query (ví dụ: "machine learning", "artificial
     * intelligence")
     * Trả về list các Map chứa raw data từ API
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchPapers(String query, int limit) {
        try {
            // Gọi endpoint /paper/search với các fields cần thiết
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/paper/search")
                            .queryParam("query", query)
                            .queryParam("limit", limit)
                            .queryParam("fields",
                                    "paperId,title,abstract,year,authors," +
                                            "externalIds,publicationVenue,fieldsOfStudy,citationCount")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Block vì SyncService chạy sync

            if (response == null || !response.containsKey("data")) {
                log.warn("Semantic Scholar trả về response rỗng cho query: {}", query);
                return Collections.emptyList();
            }

            return (List<Map<String, Object>>) response.get("data");

        } catch (Exception e) {
            log.error("Lỗi khi gọi Semantic Scholar API, query={}: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }
}
