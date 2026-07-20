package com.swp391.scientific_journal_tracker.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.dto.response.TopTopicResponse;
import com.swp391.scientific_journal_tracker.dto.response.TopicResponse;
import com.swp391.scientific_journal_tracker.service.TrendService;
import com.swp391.scientific_journal_tracker.service.TopicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final TrendService trendService;

    @GetMapping
    public List<TopicResponse> getAllTopics() {
        return topicService.getAllTopics();
    }

    @GetMapping("/search")
    public List<TopicResponse> searchTopics(@RequestParam String keyword) {
        return topicService.searchTopics(keyword);
    }

    @GetMapping("/trending")
    public List<TopTopicResponse> getTrendingTopics(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(defaultValue = "10") int limit) {
        return trendService.getTopTrendingTopics(fromYear, limit);
    }

    @GetMapping("/popular")
    public List<TopicResponse> getPopularTopics(
            @RequestParam(defaultValue = "10") int limit) {
        return topicService.getPopularTopics(limit);
    }

    @GetMapping("/following")
    public List<TopicResponse> getMyFollowingTopics(Authentication authentication) {
        return topicService.getMyFollowingTopics(authentication);
    }

    @PostMapping("/{topicId}/follow")
    public TopicResponse followTopic(
            @PathVariable Long topicId,
            Authentication authentication) {
        return topicService.followTopic(topicId, authentication);
    }

    @DeleteMapping("/{topicId}/follow")
    public TopicResponse unfollowTopic(
            @PathVariable Long topicId,
            Authentication authentication) {
        return topicService.unfollowTopic(topicId, authentication);
    }

    @GetMapping("/{topicId}/follow/check")
    public Map<String, Boolean> checkTopicFollowed(
            @PathVariable Long topicId,
            Authentication authentication) {
        boolean followed = topicService.isTopicFollowed(topicId, authentication);

        return Map.of("followed", followed);
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
