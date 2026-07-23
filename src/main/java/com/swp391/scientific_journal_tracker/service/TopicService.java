package com.swp391.scientific_journal_tracker.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.exception.DuplicateResourceException;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
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
        private final UserRepository userRepository;

        @Transactional(readOnly = true)
        public List<TopicResponse> getAllTopics() {
                return researchTopicRepository.findAllTopicSummaries()
                                .stream()
                                .map(this::toTopicResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TopicResponse> searchTopics(String keyword) {
                if (keyword == null || keyword.trim().isEmpty()) {
                        return getAllTopics();
                }

                return researchTopicRepository.searchTopicSummaries(keyword.trim())
                                .stream()
                                .map(this::toTopicResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public TopicResponse getTopicDetail(Long topicId) {
                return researchTopicRepository.findTopicSummaryById(topicId)
                                .map(this::toTopicResponse)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Topic not found with id: " + topicId));
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

        /**
         * Lấy các topic phổ biến theo tổng số paper.
         *
         * Đây là danh sách "popular topics", khác với "trending topics"
         * trong TrendService vốn được xếp hạng bằng growth rate + volume.
         */
        @Transactional(readOnly = true)
        public List<TopicResponse> getPopularTopics(int limit) {
                int safeLimit = Math.max(1, Math.min(limit, 20));

                return researchTopicRepository
                                .findPopularTopics(PageRequest.of(0, safeLimit))
                                .stream()
                                .map(this::toTopicResponse)
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

        @Transactional
        public TopicResponse followTopic(Long topicId, Authentication authentication) {
                User user = getCurrentUser(authentication);
                ResearchTopic topic = getTopicById(topicId);

                boolean alreadyFollowed = user.getFollowingTopics()
                                .stream()
                                .anyMatch(t -> t.getResearchTopicId().equals(topicId));

                if (alreadyFollowed) {
                        throw new DuplicateResourceException("Topic already followed");
                }

                user.getFollowingTopics().add(topic);

                boolean followerExists = topic.getFollowers()
                                .stream()
                                .anyMatch(u -> u.getUserId().equals(user.getUserId()));

                if (!followerExists) {
                        topic.getFollowers().add(user);
                }

                userRepository.save(user);

                return getTopicDetail(topicId);
        }

        @Transactional
        public TopicResponse unfollowTopic(Long topicId, Authentication authentication) {
                User user = getCurrentUser(authentication);
                ResearchTopic topic = getTopicById(topicId);

                boolean removed = user.getFollowingTopics()
                                .removeIf(t -> t.getResearchTopicId().equals(topicId));

                if (!removed) {
                        throw new ResourceNotFoundException("Topic follow not found");
                }

                topic.getFollowers()
                                .removeIf(u -> u.getUserId().equals(user.getUserId()));

                userRepository.save(user);

                return getTopicDetail(topicId);
        }

        @Transactional(readOnly = true)
        public List<TopicResponse> getMyFollowingTopics(Authentication authentication) {
                User user = getCurrentUser(authentication);

                return researchTopicRepository.findFollowingTopicSummaries(user.getUserId())
                                .stream()
                                .map(this::toTopicResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public boolean isTopicFollowed(Long topicId, Authentication authentication) {
                User user = getCurrentUser(authentication);

                return user.getFollowingTopics()
                                .stream()
                                .anyMatch(t -> t.getResearchTopicId().equals(topicId));
        }

        private User getCurrentUser(Authentication authentication) {
                if (authentication == null || authentication.getName() == null) {
                        throw new ResourceNotFoundException("User not authenticated");
                }

                String username = authentication.getName();

                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        private ResearchTopic getTopicById(Long topicId) {
                return researchTopicRepository.findById(topicId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Topic not found with id: " + topicId));
        }

        private TopicResponse toTopicResponse(Object[] row) {
                return new TopicResponse(
                                ((Number) row[0]).longValue(),
                                (String) row[1],
                                (String) row[2],
                                ((Number) row[3]).longValue(),
                                ((Number) row[4]).longValue());
        }
}
