package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.swp391.scientific_journal_tracker.dto.response.TopTopicResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendResponse;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendService {

        private final ResearchPaperRepository researchPaperRepository;

        public List<TrendResponse> getTrendByKeyword(String keyword) {
                return researchPaperRepository.getTrendByKeyword(keyword)
                                .stream()
                                .map(row -> new TrendResponse(
                                                (Integer) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        public List<TrendResponse> getTrendByTopic(String topic) {
                return researchPaperRepository.getTrendByTopic(topic)
                                .stream()
                                .map(row -> new TrendResponse(
                                                (Integer) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        public List<TopTopicResponse> getTopTrendingTopics(Integer fromYear, int limit) {
                int safeLimit = Math.max(1, Math.min(limit, 20));

                if (fromYear == null) {
                        fromYear = Year.now().getValue() - 5;
                }

                return researchPaperRepository.getTop5TrendingTopics(fromYear, PageRequest.of(0, safeLimit))
                                .stream()
                                .map(row -> new TopTopicResponse(
                                                (String) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }
}