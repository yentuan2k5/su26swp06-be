package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.MindMapEdgeResponse;
import com.swp391.scientific_journal_tracker.dto.response.MindMapNodeResponse;
import com.swp391.scientific_journal_tracker.dto.response.MindMapResponse;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;

import lombok.RequiredArgsConstructor;

/**
 * Tạo mind map động từ các quan hệ đã chuẩn hóa của ResearchPaper.
 * Không lưu graph vào database để dữ liệu luôn phản ánh catalog hiện tại.
 */
@Service
@RequiredArgsConstructor
public class MindMapService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final int TREND_PERIOD_YEARS = 5;

    private final KeywordRepository keywordRepository;
    private final ResearchTopicRepository researchTopicRepository;
    private final ResearchPaperRepository researchPaperRepository;

    @Transactional(readOnly = true)
    public MindMapResponse getMindMap(String type, Long id, int limit) {
        if (id == null || id <= 0) {
            throw new BadRequestException("id phải là số nguyên dương");
        }

        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        MindMapType rootType = parseType(type);
        TrendWindow trendWindow = TrendWindow.current();

        return rootType == MindMapType.KEYWORD
                ? buildKeywordMindMap(id, safeLimit, trendWindow)
                : buildTopicMindMap(id, safeLimit, trendWindow);
    }

    private MindMapResponse buildKeywordMindMap(
            Long keywordId,
            int limit,
            TrendWindow trendWindow) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("Keyword not found: " + keywordId));

        MindMapNodeResponse root = toNode(
                MindMapType.KEYWORD,
                keyword.getKeywordId(),
                keyword.getTerm(),
                researchPaperRepository.getKeywordMindMapStats(
                        keywordId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear()),
                0);

        PageRequest pageRequest = PageRequest.of(0, limit);
        List<MindMapNodeResponse> relatedTopics = toNodes(
                MindMapType.TOPIC,
                researchPaperRepository.findMindMapTopicsForKeyword(
                        keywordId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear(),
                        pageRequest));
        List<MindMapNodeResponse> relatedKeywords = toNodes(
                MindMapType.KEYWORD,
                researchPaperRepository.findMindMapKeywordsForKeyword(
                        keywordId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear(),
                        pageRequest));
        List<MindMapNodeResponse> relatedJournals = toNodes(
                MindMapType.JOURNAL,
                researchPaperRepository.findMindMapJournalsForKeyword(
                        keywordId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear(),
                        pageRequest));

        return assembleMindMap(root, relatedTopics, relatedKeywords, relatedJournals);
    }

    private MindMapResponse buildTopicMindMap(
            Long topicId,
            int limit,
            TrendWindow trendWindow) {
        ResearchTopic topic = researchTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        MindMapNodeResponse root = toNode(
                MindMapType.TOPIC,
                topic.getResearchTopicId(),
                topic.getName(),
                researchPaperRepository.getTopicMindMapStats(
                        topicId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear()),
                0);

        PageRequest pageRequest = PageRequest.of(0, limit);
        List<MindMapNodeResponse> relatedTopics = toNodes(
                MindMapType.TOPIC,
                researchPaperRepository.findMindMapTopicsForTopic(
                        topicId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear(),
                        pageRequest));
        List<MindMapNodeResponse> relatedKeywords = toNodes(
                MindMapType.KEYWORD,
                researchPaperRepository.findMindMapKeywordsForTopic(
                        topicId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear(),
                        pageRequest));
        List<MindMapNodeResponse> relatedJournals = toNodes(
                MindMapType.JOURNAL,
                researchPaperRepository.findMindMapJournalsForTopic(
                        topicId,
                        trendWindow.recentStartYear(),
                        trendWindow.currentYear(),
                        trendWindow.previousStartYear(),
                        trendWindow.previousEndYear(),
                        pageRequest));

        return assembleMindMap(root, relatedTopics, relatedKeywords, relatedJournals);
    }

    private MindMapResponse assembleMindMap(
            MindMapNodeResponse root,
            List<MindMapNodeResponse> relatedTopics,
            List<MindMapNodeResponse> relatedKeywords,
            List<MindMapNodeResponse> relatedJournals) {
        List<MindMapNodeResponse> nodes = new ArrayList<>();
        List<MindMapEdgeResponse> edges = new ArrayList<>();
        nodes.add(root);

        appendBranch(nodes, edges, root, relatedTopics, "RELATED_TOPIC");
        appendBranch(nodes, edges, root, relatedKeywords, "RELATED_KEYWORD");
        appendBranch(nodes, edges, root, relatedJournals, "PUBLISHED_IN");

        return new MindMapResponse(root, nodes, edges);
    }

    private void appendBranch(
            List<MindMapNodeResponse> nodes,
            List<MindMapEdgeResponse> edges,
            MindMapNodeResponse root,
            List<MindMapNodeResponse> children,
            String relation) {
        nodes.addAll(children);
        children.forEach(child -> edges.add(new MindMapEdgeResponse(
                root.getId(),
                child.getId(),
                relation)));
    }

    private List<MindMapNodeResponse> toNodes(
            MindMapType type,
            List<Object[]> rows) {
        return rows.stream()
                .map(row -> toNode(
                        type,
                        ((Number) row[0]).longValue(),
                        row[1] == null ? "Unknown" : row[1].toString(),
                        new Object[] { row[2], row[3], row[4] },
                        1))
                .toList();
    }

    private MindMapNodeResponse toNode(
            MindMapType type,
            Long entityId,
            String label,
            Object[] stats,
            int depth) {
        long paperCount = numberAt(stats, 0);
        long recentPaperCount = numberAt(stats, 1);
        long previousPaperCount = numberAt(stats, 2);

        return new MindMapNodeResponse(
                type.name() + ":" + entityId,
                type.name(),
                label,
                paperCount,
                recentPaperCount,
                previousPaperCount,
                resolveTrendStatus(recentPaperCount, previousPaperCount),
                depth);
    }

    private long numberAt(Object[] row, int index) {
        if (row == null || row.length <= index || !(row[index] instanceof Number number)) {
            return 0;
        }

        return number.longValue();
    }

    private String resolveTrendStatus(long recentPaperCount, long previousPaperCount) {
        if (recentPaperCount == 0 && previousPaperCount == 0) {
            return "NO_DATA";
        }

        if (previousPaperCount == 0) {
            return "EMERGING";
        }

        if (recentPaperCount > previousPaperCount) {
            return "GROWING";
        }

        if (recentPaperCount < previousPaperCount) {
            return "DECLINING";
        }

        return "STABLE";
    }

    private MindMapType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("type phải là KEYWORD hoặc TOPIC");
        }

        try {
            MindMapType mindMapType = MindMapType.valueOf(type.trim().toUpperCase(Locale.ROOT));
            if (mindMapType == MindMapType.JOURNAL) {
                throw new BadRequestException("type phải là KEYWORD hoặc TOPIC");
            }

            return mindMapType;
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("type phải là KEYWORD hoặc TOPIC");
        }
    }

    private enum MindMapType {
        KEYWORD,
        TOPIC,
        JOURNAL
    }

    private record TrendWindow(
            int recentStartYear,
            int currentYear,
            int previousStartYear,
            int previousEndYear) {

        private static TrendWindow current() {
            int currentYear = Year.now().getValue();
            int recentStartYear = currentYear - TREND_PERIOD_YEARS + 1;
            int previousEndYear = recentStartYear - 1;
            int previousStartYear = previousEndYear - TREND_PERIOD_YEARS + 1;

            return new TrendWindow(
                    recentStartYear,
                    currentYear,
                    previousStartYear,
                    previousEndYear);
        }
    }
}
