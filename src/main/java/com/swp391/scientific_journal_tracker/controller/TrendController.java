package com.swp391.scientific_journal_tracker.controller;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.TrendResponse;
import com.swp391.scientific_journal_tracker.service.TrendService;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {
    private final TrendService trendService;

    @GetMapping("/keyword")
    public List<TrendResponse> getTrendByKeyword(
            @RequestParam String keyword) {

        return trendService.getTrendByKeyword(keyword);
    }

    @GetMapping("/topic")
    public List<TrendResponse> getTrendByTopic(
            @RequestParam String topic) {

        return trendService.getTrendByTopic(topic);
    }
}
