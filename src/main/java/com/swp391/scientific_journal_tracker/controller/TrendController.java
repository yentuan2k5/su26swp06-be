package com.swp391.scientific_journal_tracker.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.TopKeywordResponse;
import com.swp391.scientific_journal_tracker.dto.response.TopTopicResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendComparisonResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendAnalysisResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendResponse;
import com.swp391.scientific_journal_tracker.security.ResearchAccessLevel;
import com.swp391.scientific_journal_tracker.security.ResearchAccessPolicy;
import com.swp391.scientific_journal_tracker.service.TrendService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cung cấp các API phân tích xu hướng bài báo khoa học.
 *
 * Hỗ trợ phân tích theo keyword, topic, lĩnh vực journal
 * và lấy danh sách topic/keyword đang tăng trưởng.
 */
@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;
    private final ResearchAccessPolicy researchAccessPolicy;

    /**
     * Trả về số lượng paper theo từng năm của một keyword.
     *
     * Ví dụ:
     * GET /api/trends/keyword?keyword=machine learning
     *
     * @param keyword keyword cần phân tích
     * @return danh sách trend theo năm
     */
    @GetMapping("/keyword")
    public ResponseEntity<List<TrendResponse>> getTrendByKeyword(
            @RequestParam String keyword) {
        return ResponseEntity.ok(
                trendService.getTrendByKeyword(keyword));
    }

    /**
     * Trả về số lượng paper theo từng năm của một topic.
     *
     * Ví dụ:
     * GET /api/trends/topic?topic=Artificial Intelligence
     *
     * @param topic topic cần phân tích
     * @return danh sách trend theo năm
     */
    @GetMapping("/topic")
    public ResponseEntity<List<TrendResponse>> getTrendByTopic(
            @RequestParam String topic) {
        return ResponseEntity.ok(
                trendService.getTrendByTopic(topic));
    }

    /**
     * Trả về kết quả trend đã tính toán cho một keyword. Student và Lecturer
     * luôn dùng cửa sổ mặc định; Researcher/Admin được tùy chọn khoảng recent.
     */
    @GetMapping("/keyword/analysis")
    @PreAuthorize("hasAnyRole('STUDENT', 'LECTURER', 'RESEARCHER', 'ADMIN')")
    public ResponseEntity<TrendAnalysisResponse> analyzeKeywordTrend(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            Authentication authentication) {
        boolean fullAccess = hasFullTrendAccess(authentication);
        return ResponseEntity.ok(trendService.analyzeKeywordTrend(
                keyword,
                fullAccess ? fromYear : null,
                fullAccess ? toYear : null));
    }

    /** Trả về kết quả trend đã tính toán cho một topic. */
    @GetMapping("/topic/analysis")
    @PreAuthorize("hasAnyRole('STUDENT', 'LECTURER', 'RESEARCHER', 'ADMIN')")
    public ResponseEntity<TrendAnalysisResponse> analyzeTopicTrend(
            @RequestParam String topic,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear,
            Authentication authentication) {
        boolean fullAccess = hasFullTrendAccess(authentication);
        return ResponseEntity.ok(trendService.analyzeTopicTrend(
                topic,
                fullAccess ? fromYear : null,
                fullAccess ? toYear : null));
    }

    /**
     * Trả về số lượng paper theo từng năm của một lĩnh vực.
     *
     * Field được lấy từ Journal.field.
     *
     * Ví dụ:
     * GET /api/trends/field?field=Computer Science
     *
     * @param field lĩnh vực cần phân tích
     * @return danh sách trend theo năm
     */
    @GetMapping("/field")
    public ResponseEntity<List<TrendResponse>> getTrendByField(
            @RequestParam String field) {
        return ResponseEntity.ok(
                trendService.getTrendByField(field));
    }

    /**
     * So sánh xu hướng công bố của nhiều keyword hoặc topic trên cùng một
     * khoảng thời gian.
     *
     * Ví dụ:
     * GET /api/trends/compare?type=KEYWORD&items=machine%20learning
     * &items=deep%20learning&fromYear=2023&toYear=2025
     *
     * @param type     KEYWORD hoặc TOPIC
     * @param items    từ 2 đến 5 tên keyword/topic, truyền lặp lại trên query string
     * @param fromYear năm bắt đầu, có thể để trống
     * @param toYear   năm kết thúc, có thể để trống
     * @return các series có cùng trục năm để frontend vẽ biểu đồ so sánh
     */
    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<TrendComparisonResponse> compareTrends(
            @RequestParam String type,
            @RequestParam(name = "items") List<String> items,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear) {
        return ResponseEntity.ok(
                trendService.compareTrends(type, items, fromYear, toYear));
    }

    /**
     * Trả về những topic có tổng số paper cao nhất
     * trong khoảng thời gian được chọn.
     *
     * @param fromYear năm bắt đầu thống kê, có thể để trống
     * @param limit    số lượng topic tối đa
     * @return danh sách topic phổ biến
     */
    @GetMapping("/top-topics")
    @PreAuthorize("hasAnyRole('LECTURER', 'RESEARCHER', 'ADMIN')")
    public ResponseEntity<List<TopTopicResponse>> getTopTrendingTopics(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        boolean basicAccess = researchAccessPolicy.resolve(authentication) == ResearchAccessLevel.BASIC;
        return ResponseEntity.ok(
                trendService.getTopTrendingTopics(
                        basicAccess ? null : fromYear,
                        basicAccess ? Math.min(limit, 5) : limit));
    }

    /**
     * Trả về những keyword đang tăng trưởng mạnh
     * trong khoảng thời gian được chọn.
     *
     * @param fromYear năm bắt đầu thống kê, có thể để trống
     * @param limit    số lượng keyword tối đa
     * @return danh sách keyword đang tăng trưởng
     */
    @GetMapping("/top-keywords")
    @PreAuthorize("hasAnyRole('LECTURER', 'RESEARCHER', 'ADMIN')")
    public ResponseEntity<List<TopKeywordResponse>> getTopTrendingKeywords(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        boolean basicAccess = researchAccessPolicy.resolve(authentication) == ResearchAccessLevel.BASIC;
        return ResponseEntity.ok(
                trendService.getTopTrendingKeywords(
                        basicAccess ? null : fromYear,
                        basicAccess ? Math.min(limit, 5) : limit));
    }

    private boolean hasFullTrendAccess(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RESEARCHER".equals(authority.getAuthority())
                        || "ROLE_ADMIN".equals(authority.getAuthority()));
    }

}
