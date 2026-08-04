package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperComparisonItemResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperComparisonResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperSimilarityResponse;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.exception.ResourceNotFoundException;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaperService {

    private static final int MIN_COMPARISON_PAPERS = 2;
    private static final int MAX_COMPARISON_PAPERS = 4;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "year",
            "citationCount",
            "title",
            "researchPaperId");

    private final ResearchPaperRepository researchPaperRepository;

    @Transactional(readOnly = true)
    public Page<PaperResponse> getPapers(
            String search,
            String author,
            String keyword,
            String journal,
            String topic,
            Integer year,
            Integer yearFrom,
            Integer yearTo,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));

        String safeSortBy = normalizeSortBy(sortBy);
        Sort.Direction direction = normalizeSortDirection(sortDir);

        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(direction, safeSortBy));

        String safeSearch = emptyToNull(search);
        String safeAuthor = emptyToNull(author);
        String safeKeyword = emptyToNull(keyword);
        String safeJournal = emptyToNull(journal);
        String safeTopic = emptyToNull(topic);

        boolean hasTextFilter = safeSearch != null
                || safeAuthor != null
                || safeKeyword != null
                || safeJournal != null
                || safeTopic != null;

        boolean hasYearFilter = year != null
                || yearFrom != null
                || yearTo != null;

        /*
         * Tách query để trang danh sách paper mặc định không phải JOIN
         * keyword / topic / journal / author. Chỉ khi người dùng search
         * hoặc dùng filter text mới chạy query nâng cao.
         */
        if (!hasTextFilter && !hasYearFilter) {
            return researchPaperRepository.findAll(pageRequest)
                    .map(PaperResponse::fromEntity);
        }

        if (!hasTextFilter) {
            return researchPaperRepository
                    .searchPapersByYearRange(
                            year,
                            yearFrom,
                            yearTo,
                            pageRequest)
                    .map(PaperResponse::fromEntity);
        }

        return researchPaperRepository
                .searchPapersAdvanced(
                        safeSearch,
                        safeAuthor,
                        safeKeyword,
                        safeJournal,
                        safeTopic,
                        year,
                        yearFrom,
                        yearTo,
                        pageRequest)
                .map(PaperResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PaperResponse getPaperById(Long id) {
        ResearchPaper paper = researchPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id: " + id));

        return PaperResponse.fromEntity(paper);
    }

    /**
     * So sánh metadata của từ hai đến bốn paper được chọn.
     *
     * Các quan hệ author, journal, keyword và topic đã được chuẩn hóa trong
     * database nên dữ liệu có thể được đối chiếu trực tiếp. Similarity chỉ dựa
     * vào keyword/topic bằng Jaccard, không suy diễn nội dung toàn văn hoặc gọi
     * dịch vụ AI bên ngoài.
     *
     * @param ids danh sách ResearchPaperId khác nhau, có từ 2 đến 4 phần tử
     * @return metadata từng paper, phần chung/riêng và similarity từng cặp
     */
    @Transactional(readOnly = true)
    public PaperComparisonResponse comparePapers(List<Long> ids) {
        List<Long> safeIds = validateComparisonIds(ids);
        Map<Long, ResearchPaper> papersById = researchPaperRepository.findAllById(safeIds)
                .stream()
                .collect(Collectors.toMap(
                        ResearchPaper::getResearchPaperId,
                        Function.identity()));

        safeIds.stream()
                .filter(id -> !papersById.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new ResourceNotFoundException("Paper not found with id: " + id);
                });

        List<PaperComparisonItemResponse> comparisonItems = safeIds.stream()
                .map(papersById::get)
                .map(this::toComparisonItem)
                .toList();

        return new PaperComparisonResponse(
                comparisonItems,
                findBestPaperId(comparisonItems, item -> safeYear(item.getPaper().getYear())),
                findBestPaperId(comparisonItems, item -> safeCitationCount(item.getPaper())),
                findBestPaperId(comparisonItems, PaperComparisonItemResponse::getCitationsPerYear),
                findCommonValues(comparisonItems, item -> item.getPaper().getKeywords()),
                findCommonValues(comparisonItems, item -> item.getPaper().getTopics()),
                buildSimilarities(comparisonItems));
    }

    private PaperComparisonItemResponse toComparisonItem(ResearchPaper researchPaper) {
        PaperResponse paper = PaperResponse.fromEntity(researchPaper);
        return new PaperComparisonItemResponse(
                paper,
                calculateCitationsPerYear(paper),
                List.of(),
                List.of());
    }

    private List<Long> validateComparisonIds(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BadRequestException("ids phải gồm từ 2 đến 4 ID dương khác nhau");
        }

        List<Long> safeIds = new ArrayList<>(new LinkedHashSet<>(ids));
        if (safeIds.size() != ids.size()
                || safeIds.size() < MIN_COMPARISON_PAPERS
                || safeIds.size() > MAX_COMPARISON_PAPERS) {
            throw new BadRequestException("ids phải gồm từ 2 đến 4 ID dương khác nhau");
        }

        return safeIds;
    }

    private double calculateCitationsPerYear(PaperResponse paper) {
        int publicationYear = safeYear(paper.getYear());
        int yearsSincePublication = publicationYear == 0
                ? 1
                : Math.max(1, Year.now().getValue() - publicationYear + 1);
        double citationsPerYear = (double) safeCitationCount(paper) / yearsSincePublication;

        return Math.round(citationsPerYear * 100.0) / 100.0;
    }

    private Long findBestPaperId(
            List<PaperComparisonItemResponse> items,
            ToDoubleFunction<PaperComparisonItemResponse> scoreExtractor) {
        return items.stream()
                .max(Comparator.comparingDouble(scoreExtractor))
                .map(item -> item.getPaper().getResearchPaperId())
                .orElse(null);
    }

    private List<String> findCommonValues(
            List<PaperComparisonItemResponse> items,
            Function<PaperComparisonItemResponse, List<String>> valuesExtractor) {
        Set<String> commonNormalizedValues = null;
        Map<String, String> displayValues = new LinkedHashMap<>();

        for (PaperComparisonItemResponse item : items) {
            Map<String, String> normalizedValues = normalizeValues(valuesExtractor.apply(item));
            displayValues.putAll(normalizedValues);

            if (commonNormalizedValues == null) {
                commonNormalizedValues = new HashSet<>(normalizedValues.keySet());
            } else {
                commonNormalizedValues.retainAll(normalizedValues.keySet());
            }
        }

        if (commonNormalizedValues == null) {
            return List.of();
        }

        return commonNormalizedValues.stream()
                .map(displayValues::get)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<PaperSimilarityResponse> buildSimilarities(
            List<PaperComparisonItemResponse> items) {
        applyUniqueMetadata(items);

        List<PaperSimilarityResponse> similarities = new ArrayList<>();
        for (int firstIndex = 0; firstIndex < items.size(); firstIndex++) {
            for (int secondIndex = firstIndex + 1; secondIndex < items.size(); secondIndex++) {
                PaperComparisonItemResponse firstPaper = items.get(firstIndex);
                PaperComparisonItemResponse secondPaper = items.get(secondIndex);

                Set<String> firstKeywords = normalizeValues(firstPaper.getPaper().getKeywords()).keySet();
                Set<String> secondKeywords = normalizeValues(secondPaper.getPaper().getKeywords()).keySet();
                Set<String> firstTopics = normalizeValues(firstPaper.getPaper().getTopics()).keySet();
                Set<String> secondTopics = normalizeValues(secondPaper.getPaper().getTopics()).keySet();

                double keywordSimilarity = calculateJaccardSimilarity(firstKeywords, secondKeywords);
                double topicSimilarity = calculateJaccardSimilarity(firstTopics, secondTopics);

                Set<String> firstMetadata = new HashSet<>();
                firstKeywords.forEach(keyword -> firstMetadata.add("KEYWORD:" + keyword));
                firstTopics.forEach(topic -> firstMetadata.add("TOPIC:" + topic));

                Set<String> secondMetadata = new HashSet<>();
                secondKeywords.forEach(keyword -> secondMetadata.add("KEYWORD:" + keyword));
                secondTopics.forEach(topic -> secondMetadata.add("TOPIC:" + topic));

                similarities.add(new PaperSimilarityResponse(
                        firstPaper.getPaper().getResearchPaperId(),
                        secondPaper.getPaper().getResearchPaperId(),
                        keywordSimilarity,
                        topicSimilarity,
                        calculateJaccardSimilarity(firstMetadata, secondMetadata)));
            }
        }

        return similarities;
    }

    private void applyUniqueMetadata(List<PaperComparisonItemResponse> items) {
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            PaperComparisonItemResponse item = items.get(itemIndex);
            item.setUniqueKeywords(findUniqueValues(itemIndex, items,
                    currentItem -> currentItem.getPaper().getKeywords()));
            item.setUniqueTopics(findUniqueValues(itemIndex, items,
                    currentItem -> currentItem.getPaper().getTopics()));
        }
    }

    private List<String> findUniqueValues(
            int itemIndex,
            List<PaperComparisonItemResponse> items,
            Function<PaperComparisonItemResponse, List<String>> valuesExtractor) {
        Map<String, String> currentValues = normalizeValues(valuesExtractor.apply(items.get(itemIndex)));
        Set<String> valuesInOtherPapers = new HashSet<>();

        for (int index = 0; index < items.size(); index++) {
            if (index != itemIndex) {
                valuesInOtherPapers.addAll(normalizeValues(valuesExtractor.apply(items.get(index))).keySet());
            }
        }

        return currentValues.entrySet().stream()
                .filter(entry -> !valuesInOtherPapers.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Map<String, String> normalizeValues(List<String> values) {
        if (values == null) {
            return Map.of();
        }

        Map<String, String> normalizedValues = new LinkedHashMap<>();
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(value -> normalizedValues.putIfAbsent(
                        value.toLowerCase(Locale.ROOT),
                        value));

        return normalizedValues;
    }

    private double calculateJaccardSimilarity(Set<String> firstValues, Set<String> secondValues) {
        Set<String> union = new HashSet<>(firstValues);
        union.addAll(secondValues);
        if (union.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(firstValues);
        intersection.retainAll(secondValues);
        return roundSimilarity((double) intersection.size() / union.size());
    }

    private double roundSimilarity(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private int safeYear(Integer year) {
        return year == null ? 0 : year;
    }

    private int safeCitationCount(PaperResponse paper) {
        return paper.getCitationCount() == null ? 0 : Math.max(0, paper.getCitationCount());
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "year";
        }

        String normalized = sortBy.trim();

        if (!ALLOWED_SORT_FIELDS.contains(normalized)) {
            return "year";
        }

        return normalized;
    }

    private Sort.Direction normalizeSortDirection(String sortDir) {
        if (sortDir == null || sortDir.trim().isEmpty()) {
            return Sort.Direction.DESC;
        }

        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }

        return Sort.Direction.DESC;
    }
}
