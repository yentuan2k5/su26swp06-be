package com.swp391.scientific_journal_tracker.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.dto.response.TopicResponse;
import com.swp391.scientific_journal_tracker.service.TopicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public List<TopicResponse> getAllTopics() {
        return topicService.getAllTopics();
    }

    @GetMapping("/search")
    public List<TopicResponse> searchTopics(@RequestParam String keyword) {
        return topicService.searchTopics(keyword);
    }

    @GetMapping("/trending")
    public List<TopicResponse> getTrendingTopics(
            @RequestParam(defaultValue = "10") int limit) {
        return topicService.getTrendingTopics(limit);
    }

    @GetMapping("/{topicId}")
    public TopicResponse getTopicDetail(@PathVariable Long topicId) {
        return topicService.getTopicDetail(topicId);
    }

    @GetMapping("/{topicId}/papers")
    public Page<PaperResponse> getPapersByTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return topicService.getPapersByTopic(topicId, page, size);
    }
}