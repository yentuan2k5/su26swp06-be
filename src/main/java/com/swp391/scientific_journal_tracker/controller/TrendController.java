package com.swp391.scientific_journal_tracker.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.TopTopicResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendResponse;
import com.swp391.scientific_journal_tracker.service.TrendService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cung cấp các API phân tích xu hướng bài báo khoa học.
 *
 * Hỗ trợ phân tích theo keyword, topic, lĩnh vực journal
 * và lấy danh sách topic phổ biến.
 */
@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

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
     * Trả về những topic có tổng số paper cao nhất
     * trong khoảng thời gian được chọn.
     *
     * @param fromYear năm bắt đầu thống kê, có thể để trống
     * @param limit    số lượng topic tối đa
     * @return danh sách topic phổ biến
     */
    @GetMapping("/top-topics")
    public ResponseEntity<List<TopTopicResponse>> getTopTrendingTopics(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                trendService.getTopTrendingTopics(
                        fromYear,
                        limit));
    }
}