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

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

    @GetMapping("/keyword")
    public ResponseEntity<List<TrendResponse>> getTrendByKeyword(
            @RequestParam String keyword) {
        return ResponseEntity.ok(trendService.getTrendByKeyword(keyword));
    }

    @GetMapping("/topic")
    public ResponseEntity<List<TrendResponse>> getTrendByTopic(
            @RequestParam String topic) {
        return ResponseEntity.ok(trendService.getTrendByTopic(topic));
    }

    @GetMapping("/top-topics")
    public ResponseEntity<List<TopTopicResponse>> getTopTrendingTopics(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(trendService.getTopTrendingTopics(fromYear, limit));
    }
}