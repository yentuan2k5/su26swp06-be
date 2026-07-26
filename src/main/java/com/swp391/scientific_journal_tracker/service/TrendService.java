package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.TopKeywordResponse;
import com.swp391.scientific_journal_tracker.dto.response.TopTopicResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendResponse;
import com.swp391.scientific_journal_tracker.exception.BadRequestException;
import com.swp391.scientific_journal_tracker.repository.ResearchPaperRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các chức năng phân tích xu hướng công bố.
 *
 * Trend hiện tại được tính động trực tiếp từ dữ liệu ResearchPaper,
 * bao gồm trend theo keyword, topic và lĩnh vực của journal.
 */
@Service
@RequiredArgsConstructor
public class TrendService {

        private final ResearchPaperRepository researchPaperRepository;

        private static final int DEFAULT_TREND_PERIOD_YEARS = 5;
        private static final String TREND_TYPE_GROWING = "GROWING";
        private static final String TREND_TYPE_EMERGING = "EMERGING";

        @Value("${trend.min-papers-threshold:30}")
        private int minPapersThreshold;

        @Value("${trend.min-previous-papers-threshold:5}")
        private int minPreviousPapersThreshold;

        @Value("${trend.min-recent-papers-for-emerging:30}")
        private int minRecentPapersForEmerging;

        @Value("${trend.excluded-keywords:computer science}")
        private String excludedKeywords;

        /**
         * Lấy số lượng paper theo từng năm cho một keyword.
         *
         * @param keyword keyword mà người dùng muốn phân tích
         * @return danh sách trend gồm năm và số lượng paper
         */
        @Transactional(readOnly = true)
        public List<TrendResponse> getTrendByKeyword(String keyword) {
                return researchPaperRepository
                                .getTrendByKeyword(keyword)
                                .stream()
                                .map(row -> new TrendResponse(
                                                (Integer) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        /**
         * Lấy số lượng paper theo từng năm cho một research topic.
         *
         * @param topic topic mà người dùng muốn phân tích
         * @return danh sách trend gồm năm và số lượng paper
         */
        @Transactional(readOnly = true)
        public List<TrendResponse> getTrendByTopic(String topic) {
                return researchPaperRepository
                                .getTrendByTopic(topic)
                                .stream()
                                .map(row -> new TrendResponse(
                                                (Integer) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        /**
         * Lấy số lượng paper theo từng năm cho một lĩnh vực.
         *
         * Field được lưu trong Journal nên repository sẽ join từ
         * ResearchPaper sang Journal để thực hiện thống kê.
         *
         * Kiểm tra field rỗng để tránh trường hợp LIKE '%%' làm API
         * trả về tổng số paper của tất cả lĩnh vực.
         *
         * @param field lĩnh vực cần phân tích, ví dụ "Computer Science"
         * @return danh sách trend gồm năm và số lượng paper
         */
        @Transactional(readOnly = true)
        public List<TrendResponse> getTrendByField(String field) {
                if (field == null || field.isBlank()) {
                        throw new BadRequestException(
                                        "Field không được để trống");
                }

                return researchPaperRepository
                                .getTrendByField(field.trim())
                                .stream()
                                .map(row -> new TrendResponse(
                                                (Integer) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        /**
         * @param fromYear năm bắt đầu thống kê
         * @param limit    số lượng topic tối đa
         * @return danh sách topic, số paper gần đây, growth rate, tổng paper, score và
         *         trendType
         */
        @Transactional(readOnly = true)
        public List<TopTopicResponse> getTopTrendingTopics(
                        Integer fromYear,
                        int limit) {
                int safeLimit = Math.max(
                                1,
                                Math.min(limit, 20));
                int currentYear = Year.now().getValue();
                int recentStartYear = fromYear == null
                                ? currentYear - DEFAULT_TREND_PERIOD_YEARS + 1
                                : fromYear;

                if (recentStartYear > currentYear) {
                        throw new BadRequestException(
                                        "fromYear không được lớn hơn năm hiện tại");
                }

                int periodYears = currentYear - recentStartYear + 1;
                int previousEndYear = recentStartYear - 1;
                int previousStartYear = recentStartYear - periodYears;
                int safeMinPapersThreshold = Math.max(
                                0,
                                minPapersThreshold);

                return researchPaperRepository
                                .getTopicGrowthStats(
                                                recentStartYear,
                                                previousStartYear,
                                                previousEndYear)
                                .stream()
                                .map(this::toTopTopicResponse)
                                .filter(response -> response.getTotalPapers() >= safeMinPapersThreshold)
                                .sorted((left, right) -> Double.compare(
                                                right.getScore(),
                                                left.getScore()))
                                .limit(safeLimit)
                                .toList();
        }

        /**
         * @param fromYear năm bắt đầu thống kê
         * @param limit    số lượng keyword tối đa
         * @return danh sách keyword, số paper gần đây, growth rate, tổng paper, score
         *         và trendType
         */
        @Transactional(readOnly = true)
        public List<TopKeywordResponse> getTopTrendingKeywords(
                        Integer fromYear,
                        int limit) {
                int safeLimit = Math.max(
                                1,
                                Math.min(limit, 20));
                int currentYear = Year.now().getValue();
                int recentStartYear = fromYear == null
                                ? currentYear - DEFAULT_TREND_PERIOD_YEARS + 1
                                : fromYear;

                if (recentStartYear > currentYear) {
                        throw new BadRequestException(
                                        "fromYear không được lớn hơn năm hiện tại");
                }

                int periodYears = currentYear - recentStartYear + 1;
                int previousEndYear = recentStartYear - 1;
                int previousStartYear = recentStartYear - periodYears;
                int safeMinPapersThreshold = Math.max(
                                0,
                                minPapersThreshold);
                Set<String> safeExcludedKeywords = parseExcludedKeywords();

                return researchPaperRepository
                                .getKeywordGrowthStats(
                                                recentStartYear,
                                                previousStartYear,
                                                previousEndYear)
                                .stream()
                                .map(this::toTopKeywordResponse)
                                .filter(response -> !safeExcludedKeywords
                                                .contains(normalizeKeyword(response.getKeyword())))
                                .filter(response -> response.getTotalPapers() >= safeMinPapersThreshold)
                                .sorted((left, right) -> Double.compare(
                                                right.getScore(),
                                                left.getScore()))
                                .limit(safeLimit)
                                .toList();
        }

        private TopTopicResponse toTopTopicResponse(Object[] row) {
                long recentCount = ((Number) row[1]).longValue();
                long previousCount = ((Number) row[2]).longValue();
                long totalPapers = recentCount + previousCount;
                double growthRate = calculateGrowthRate(
                                recentCount,
                                previousCount);
                double score = calculateTrendScore(
                                growthRate,
                                totalPapers);

                return new TopTopicResponse(
                                (String) row[0],
                                recentCount,
                                growthRate,
                                totalPapers,
                                score,
                                resolveTrendType(
                                                recentCount,
                                                previousCount));
        }

        private TopKeywordResponse toTopKeywordResponse(Object[] row) {
                long recentCount = ((Number) row[1]).longValue();
                long previousCount = ((Number) row[2]).longValue();
                long totalPapers = recentCount + previousCount;
                double growthRate = calculateGrowthRate(
                                recentCount,
                                previousCount);
                double score = calculateTrendScore(
                                growthRate,
                                totalPapers);

                return new TopKeywordResponse(
                                (String) row[0],
                                recentCount,
                                growthRate,
                                totalPapers,
                                score,
                                resolveTrendType(
                                                recentCount,
                                                previousCount));
        }

        private String resolveTrendType(
                        long recentCount,
                        long previousCount) {
                int safeMinPreviousPapersThreshold = Math.max(
                                0,
                                minPreviousPapersThreshold);
                int safeMinRecentPapersForEmerging = Math.max(
                                1,
                                minRecentPapersForEmerging);

                if (previousCount < safeMinPreviousPapersThreshold
                                && recentCount >= safeMinRecentPapersForEmerging) {
                        return TREND_TYPE_EMERGING;
                }

                return TREND_TYPE_GROWING;
        }

        private Set<String> parseExcludedKeywords() {
                if (excludedKeywords == null || excludedKeywords.isBlank()) {
                        return Set.of();
                }

                return Arrays.stream(excludedKeywords.split(","))
                                .map(this::normalizeKeyword)
                                .filter(keyword -> !keyword.isBlank())
                                .collect(Collectors.toSet());
        }

        private String normalizeKeyword(String keyword) {
                return keyword == null
                                ? ""
                                : keyword.trim().toLowerCase();
        }

        private double calculateGrowthRate(
                        long recentCount,
                        long previousCount) {
                if (previousCount == 0) {
                        return recentCount > 0 ? 1.0 : 0.0;
                }

                return (double) (recentCount - previousCount) / previousCount;
        }

        private double calculateTrendScore(
                        double growthRate,
                        long totalPapers) {
                return growthRate * Math.log1p(totalPapers);
        }
}
