package com.swp391.scientific_journal_tracker.service;

import java.util.List;

import org.springframework.stereotype.Service;

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
}
