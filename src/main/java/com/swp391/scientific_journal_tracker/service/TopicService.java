package com.swp391.scientific_journal_tracker.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.dto.response.TopicResponse;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final ResearchTopicRepository researchTopicRepository;
    private final ResearchPaperRepository researchPaperRepository;

    @Transactional(readOnly = true)
    public List<TopicResponse> getAllTopics() {
        return researchTopicRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(TopicResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> searchTopics(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTopics();
        }

        return researchTopicRepository.searchTopics(keyword.trim())
                .stream()
                .map(TopicResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopicDetail(Long topicId) {
        ResearchTopic topic = getTopicById(topicId);
        return TopicResponse.fromEntity(topic);
    }

    @Transactional(readOnly = true)
    public Page<PaperResponse> getPapersByTopicPage(Long topicId, int page, int size) {
        ResearchTopic topic = getTopicById(topicId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("year").descending());

        return researchPaperRepository
                .searchPapersAdvanced(
                        null, // search
                        null, // author
                        null, // keyword
                        null, // journal
                        topic.getName(), // topic
                        null, // year
                        null, // yearFrom
                        null, // yearTo
                        pageable)
                .map(PaperResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getTrendingTopics(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        return researchTopicRepository
                .findTrendingTopics(PageRequest.of(0, safeLimit))
                .stream()
                .map(row -> new TopicResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()))
                .toList();
    }

    public Page<PaperResponse> getPapersByTopic(Long topicId, int page, int size) {
        ResearchTopic topic = getTopicById(topicId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("year").descending());

        return researchPaperRepository
                .searchPapersAdvanced(
                        null,
                        null,
                        null,
                        null,
                        topic.getName(),
                        null,
                        null,
                        null,
                        pageable)
                .map(PaperResponse::fromEntity);
    }

    private ResearchTopic getTopicById(Long topicId) {
        return researchTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + topicId));
    }
}