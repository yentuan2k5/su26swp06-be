package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.MindMapEdgeResponse;
import com.swp391.scientific_journal_tracker.dto.response.MindMapNodeResponse;
import com.swp391.scientific_journal_tracker.dto.response.MindMapResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.KeywordRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;
import com.swp391.scientific_journal_tracker.repository.ResearchTopicRepository;

import lombok.RequiredArgsConstructor;

/**
 * Tao mind map dong tu cac quan he da chuan hoa cua ResearchPaper.
 *
 * Moi canh la mot quan he dong xuat hien co bang chung. Canh duoc xep hang
 * theo association score = shared / sqrt(rootCatalogCount * targetCatalogCount),
 * sau do ket hop log(shared + 1). Cach nay uu tien lien he dac trung, thay vi
 * chi uu tien node lon nhat trong catalog.
 */
@Service
@RequiredArgsConstructor
public class MindMapService {

    private static final int MAX_LIMIT = 10;
    private static final int CANDIDATE_LIMIT = 50;
    private static final int TREND_PERIOD_YEARS = 5;
    private static final int MIN_SHARED_PAPERS = 3;

    private final KeywordRepository keywordRepository;
    private final ResearchTopicRepository researchTopicRepository;
    private final ResearchPaperRepository researchPaperRepository;

    @Transactional(readOnly = true)
    public MindMapResponse getMindMap(String type, Long id, int limit) {
        return getMindMap(type, id, limit, null, null);
    }

    /**
     * Tao mind map trong khoang nam nguoi dung chon. Cac so recent/previous
     * tren node va edge phan anh quan he dong xuat hien, khong phai chi tong
     * so paper cua mot node doc lap.
     */
    @Transactional(readOnly = true)
    public MindMapResponse getMindMap(
            String type,
            Long id,
            int limit,
            Integer fromYear,
            Integer toYear) {
        if (id == null || id <= 0) {
            throw new BadRequestException("id phải là số nguyên dương");
        }

        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        MindMapType rootType = parseRootType(type);
        TrendWindow trendWindow = TrendWindow.of(fromYear, toYear);

        return rootType == MindMapType.KEYWORD
                ? buildKeywordMindMap(id, safeLimit, trendWindow)
                : buildTopicMindMap(id, safeLimit, trendWindow);
    }

    private MindMapResponse buildKeywordMindMap(Long keywordId, int limit, TrendWindow trendWindow) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResourceNotFoundException("Keyword not found: " + keywordId));
        MindMapNodeResponse root = toRootNode(
                MindMapType.KEYWORD,
                keyword.getKeywordId(),
                keyword.getTerm(),
                researchPaperRepository.getKeywordMindMapStats(
                        keywordId,
                        trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear()));

        PageRequest candidatesPage = PageRequest.of(0, CANDIDATE_LIMIT);
        return assembleMindMap(
                root,
                toCandidates(MindMapType.TOPIC, researchPaperRepository.findMindMapTopicsForKeyword(
                        keywordId, trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear(), candidatesPage)),
                toCandidates(MindMapType.KEYWORD, researchPaperRepository.findMindMapKeywordsForKeyword(
                        keywordId, trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear(), candidatesPage)),
                toCandidates(MindMapType.JOURNAL, researchPaperRepository.findMindMapJournalsForKeyword(
                        keywordId, trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear(), candidatesPage)),
                limit,
                trendWindow);
    }

    private MindMapResponse buildTopicMindMap(Long topicId, int limit, TrendWindow trendWindow) {
        ResearchTopic topic = researchTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        MindMapNodeResponse root = toRootNode(
                MindMapType.TOPIC,
                topic.getResearchTopicId(),
                topic.getName(),
                researchPaperRepository.getTopicMindMapStats(
                        topicId,
                        trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear()));

        PageRequest candidatesPage = PageRequest.of(0, CANDIDATE_LIMIT);
        return assembleMindMap(
                root,
                toCandidates(MindMapType.TOPIC, researchPaperRepository.findMindMapTopicsForTopic(
                        topicId, trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear(), candidatesPage)),
                toCandidates(MindMapType.KEYWORD, researchPaperRepository.findMindMapKeywordsForTopic(
                        topicId, trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear(), candidatesPage)),
                toCandidates(MindMapType.JOURNAL, researchPaperRepository.findMindMapJournalsForTopic(
                        topicId, trendWindow.recentStartYear(), trendWindow.currentYear(),
                        trendWindow.previousStartYear(), trendWindow.previousEndYear(), candidatesPage)),
                limit,
                trendWindow);
    }

    private MindMapResponse assembleMindMap(
            MindMapNodeResponse root,
            List<RelationCandidate> relatedTopics,
            List<RelationCandidate> relatedKeywords,
            List<RelationCandidate> relatedJournals,
            int limit,
            TrendWindow trendWindow) {
        List<MindMapNodeResponse> nodes = new ArrayList<>();
        List<MindMapEdgeResponse> edges = new ArrayList<>();
        nodes.add(root);

        appendBranch(nodes, edges, root, relatedTopics, "RELATED_TOPIC", limit);
        appendBranch(nodes, edges, root, relatedKeywords, "RELATED_KEYWORD", limit);
        appendBranch(nodes, edges, root, relatedJournals, "PUBLISHED_IN", limit);

        return new MindMapResponse(
                root, nodes, edges,
                trendWindow.recentStartYear(), trendWindow.currentYear(),
                trendWindow.previousStartYear(), trendWindow.previousEndYear());
    }

    private void appendBranch(
            List<MindMapNodeResponse> nodes,
            List<MindMapEdgeResponse> edges,
            MindMapNodeResponse root,
            List<RelationCandidate> candidates,
            String relation,
            int limit) {
        Map<Long, Long> catalogCounts = loadCatalogCounts(
                candidates.isEmpty() ? MindMapType.KEYWORD : candidates.getFirst().type(),
                candidates.stream().map(RelationCandidate::entityId).toList());

        candidates.stream()
                .filter(candidate -> candidate.sharedPaperCount() >= MIN_SHARED_PAPERS)
                .map(candidate -> toRelation(
                        root,
                        candidate,
                        catalogCounts.getOrDefault(candidate.entityId(), candidate.sharedPaperCount()),
                        relation))
                .sorted(Comparator.comparingDouble(Relation::rankScore).reversed()
                        .thenComparing(Comparator.comparingLong(Relation::sharedPaperCount).reversed())
                        .thenComparing(item -> item.node().getLabel(), String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .forEach(item -> {
                    nodes.add(item.node());
                    edges.add(item.edge());
                });
    }

    private List<RelationCandidate> toCandidates(MindMapType type, List<Object[]> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(row -> new RelationCandidate(
                        type,
                        numberAt(row, 0),
                        row[1] == null ? "Unknown" : row[1].toString(),
                        numberAt(row, 2),
                        numberAt(row, 3),
                        numberAt(row, 4)))
                .toList();
    }

    private Relation toRelation(
            MindMapNodeResponse root,
            RelationCandidate candidate,
            long targetCatalogPaperCount,
            String relation) {
        double associationScore = associationScore(
                candidate.sharedPaperCount(), root.getCatalogPaperCount(), targetCatalogPaperCount);
        double rankScore = associationScore * Math.log1p(candidate.sharedPaperCount());
        double growthRate = growthRate(candidate.recentPaperCount(), candidate.previousPaperCount());
        String trendStatus = resolveTrendStatus(candidate.recentPaperCount(), candidate.previousPaperCount());
        MindMapNodeResponse node = new MindMapNodeResponse(
                candidate.type().name() + ":" + candidate.entityId(),
                candidate.type().name(),
                candidate.label(),
                candidate.sharedPaperCount(),
                candidate.recentPaperCount(),
                candidate.previousPaperCount(),
                trendStatus,
                1,
                targetCatalogPaperCount,
                candidate.sharedPaperCount());
        MindMapEdgeResponse edge = new MindMapEdgeResponse(
                root.getId(), node.getId(), relation,
                candidate.sharedPaperCount(),
                candidate.recentPaperCount(), candidate.previousPaperCount(),
                growthRate, trendStatus, associationScore, rankScore);
        return new Relation(node, edge, rankScore, candidate.sharedPaperCount());
    }

    private MindMapNodeResponse toRootNode(MindMapType type, Long entityId, String label, Object[] stats) {
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
                0,
                paperCount,
                paperCount);
    }

    /** Chuan hoa quan he dong xuat hien theo quy mo hai node. */
    private double associationScore(long sharedPaperCount, long rootPaperCount, long targetPaperCount) {
        if (sharedPaperCount <= 0 || rootPaperCount <= 0 || targetPaperCount <= 0) {
            return 0.0;
        }
        return sharedPaperCount / Math.sqrt((double) rootPaperCount * targetPaperCount);
    }

    private double growthRate(long recentPaperCount, long previousPaperCount) {
        if (previousPaperCount == 0) {
            return recentPaperCount > 0 ? 1.0 : 0.0;
        }
        return (double) (recentPaperCount - previousPaperCount) / previousPaperCount;
    }

    private Map<Long, Long> loadCatalogCounts(MindMapType type, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = switch (type) {
            case KEYWORD -> researchPaperRepository.countPapersByKeywordIds(ids);
            case TOPIC -> researchPaperRepository.countPapersByTopicIds(ids);
            case JOURNAL -> researchPaperRepository.countPapersByJournalIds(ids);
        };
        if (rows == null) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        rows.forEach(row -> result.put(numberAt(row, 0), numberAt(row, 1)));
        return result;
    }

    /**
     * Tra ve cac paper chung de nguoi dung kiem chung mot canh Mind Map.
     * Khong suy dien quan he tu text; bang chung la cac paper co ca hai metadata.
     */
    @Transactional(readOnly = true)
    public Page<PaperResponse> getEvidencePapers(
            String rootType,
            Long rootId,
            String targetType,
            Long targetId,
            int page,
            int size) {
        if (rootId == null || rootId <= 0 || targetId == null || targetId <= 0) {
            throw new BadRequestException("rootId và targetId phải là số nguyên dương");
        }
        MindMapType safeRootType = parseRootType(rootType);
        MindMapType safeTargetType = parseAnyType(targetType);
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 20)),
                Sort.by(Sort.Direction.DESC, "citationCount").and(Sort.by(Sort.Direction.DESC, "year")));

        Page<ResearchPaper> papers = switch (safeRootType) {
            case KEYWORD -> switch (safeTargetType) {
                case KEYWORD -> researchPaperRepository.findMindMapEvidenceKeywordToKeyword(rootId, targetId, pageable);
                case TOPIC -> researchPaperRepository.findMindMapEvidenceKeywordToTopic(rootId, targetId, pageable);
                case JOURNAL -> researchPaperRepository.findMindMapEvidenceKeywordToJournal(rootId, targetId, pageable);
            };
            case TOPIC -> switch (safeTargetType) {
                case KEYWORD -> researchPaperRepository.findMindMapEvidenceTopicToKeyword(rootId, targetId, pageable);
                case TOPIC -> researchPaperRepository.findMindMapEvidenceTopicToTopic(rootId, targetId, pageable);
                case JOURNAL -> researchPaperRepository.findMindMapEvidenceTopicToJournal(rootId, targetId, pageable);
            };
            case JOURNAL -> throw new BadRequestException("rootType phải là KEYWORD hoặc TOPIC");
        };
        return papers.map(PaperResponse::fromEntity);
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

    private MindMapType parseRootType(String type) {
        MindMapType mindMapType = parseAnyType(type);
        if (mindMapType == MindMapType.JOURNAL) {
            throw new BadRequestException("type phải là KEYWORD hoặc TOPIC");
        }
        return mindMapType;
    }

    private MindMapType parseAnyType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("type phải là KEYWORD, TOPIC hoặc JOURNAL");
        }
        try {
            return MindMapType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("type phải là KEYWORD, TOPIC hoặc JOURNAL");
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

        private static TrendWindow of(Integer fromYear, Integer toYear) {
            int currentYear = Year.now().getValue();
            int safeToYear = toYear == null ? currentYear : toYear;
            int recentStartYear = fromYear == null ? safeToYear - TREND_PERIOD_YEARS + 1 : fromYear;
            if (recentStartYear < 1900 || safeToYear > currentYear || recentStartYear > safeToYear) {
                throw new BadRequestException("Khoảng năm Mind Map không hợp lệ");
            }
            int periodYears = safeToYear - recentStartYear + 1;
            int previousEndYear = recentStartYear - 1;
            int previousStartYear = previousEndYear - periodYears + 1;
            return new TrendWindow(recentStartYear, safeToYear, previousStartYear, previousEndYear);
        }
    }

    private record RelationCandidate(
            MindMapType type,
            Long entityId,
            String label,
            long sharedPaperCount,
            long recentPaperCount,
            long previousPaperCount) {
    }

    private record Relation(
            MindMapNodeResponse node,
            MindMapEdgeResponse edge,
            double rankScore,
            long sharedPaperCount) {
    }
}
