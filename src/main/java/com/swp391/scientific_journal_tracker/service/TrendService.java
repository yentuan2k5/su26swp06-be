package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp391.scientific_journal_tracker.dto.response.TopKeywordResponse;
import com.swp391.scientific_journal_tracker.dto.response.TopTopicResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendComparisonResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendComparisonSeriesResponse;
import com.swp391.scientific_journal_tracker.dto.response.TrendAnalysisResponse;
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
        private static final int MIN_COMPARE_ITEMS = 2;
        private static final int MAX_COMPARE_ITEMS = 5;
        private static final String TREND_TYPE_GROWING = "GROWING";
        private static final String TREND_TYPE_EMERGING = "EMERGING";
        private static final String TREND_TYPE_STABLE = "STABLE";
        private static final String TREND_TYPE_DECLINING = "DECLINING";
        private static final String TREND_TYPE_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

        @Value("${trend.min-papers-threshold:30}")
        private int minPapersThreshold;

        @Value("${trend.min-previous-papers-threshold:5}")
        private int minPreviousPapersThreshold;

        @Value("${trend.min-recent-papers-for-emerging:30}")
        private int minRecentPapersForEmerging;

        @Value("${trend.stable-growth-rate:0.10}")
        private double stableGrowthRate;

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
         * Phân tích trend của một keyword bằng hai giai đoạn liên tiếp có cùng
         * độ dài. Số lượng paper theo năm vẫn được trả về làm bằng chứng cho
         * biểu đồ, nhưng growthRate và trendScore mới là kết luận xu hướng.
         */
        @Transactional(readOnly = true)
        public TrendAnalysisResponse analyzeKeywordTrend(
                        String keyword,
                        Integer recentFromYear,
                        Integer recentToYear) {
                return analyzeSingleTrend(
                                "KEYWORD",
                                keyword,
                                getTrendByKeyword(keyword),
                                recentFromYear,
                                recentToYear);
        }

        /** Phân tích trend của một research topic theo cùng công thức keyword. */
        @Transactional(readOnly = true)
        public TrendAnalysisResponse analyzeTopicTrend(
                        String topic,
                        Integer recentFromYear,
                        Integer recentToYear) {
                return analyzeSingleTrend(
                                "TOPIC",
                                topic,
                                getTrendByTopic(topic),
                                recentFromYear,
                                recentToYear);
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

        /**
         * So sánh xu hướng công bố của từ hai đến năm keyword hoặc topic trong
         * cùng một khoảng năm. Mỗi series luôn có đủ các năm trong khoảng đã
         * chọn; năm không có bài được trả về paperCount = 0 để frontend vẽ
         * biểu đồ nhiều đường trên cùng một trục thời gian.
         *
         * Growth rate của mỗi series được tính từ năm đầu đến năm cuối:
         * (lastYearCount - firstYearCount) / firstYearCount. Nếu năm đầu bằng
         * 0 nhưng năm cuối có bài, hệ thống quy ước growth rate là 1.0 để tránh
         * chia cho 0, tương ứng mức tăng 100%.
         *
         * @param type     KEYWORD hoặc TOPIC
         * @param itemNames tên chính xác của các keyword/topic cần so sánh
         * @param fromYear năm bắt đầu, mặc định là năm đầu của 5 năm gần đây
         * @param toYear   năm kết thúc, mặc định là năm hiện tại
         * @return dữ liệu chuỗi theo năm và growth rate của từng mục
         */
        @Transactional(readOnly = true)
        public TrendComparisonResponse compareTrends(
                        String type,
                        List<String> itemNames,
                        Integer fromYear,
                        Integer toYear) {
                int currentYear = Year.now().getValue();
                int safeFromYear = fromYear == null
                                ? currentYear - DEFAULT_TREND_PERIOD_YEARS + 1
                                : fromYear;
                int safeToYear = toYear == null ? currentYear : toYear;

                validateComparisonYears(safeFromYear, safeToYear, currentYear);
                LinkedHashMap<String, String> requestedNames = normalizeComparisonNames(itemNames);
                String safeType = normalizeComparisonType(type);

                int periodYears = safeToYear - safeFromYear + 1;
                int previousFromYear = safeFromYear - periodYears;
                validatePreviousPeriod(previousFromYear);

                List<Object[]> rows = "KEYWORD".equals(safeType)
                                ? researchPaperRepository.getKeywordComparisonTrends(
                                                new ArrayList<>(requestedNames.keySet()),
                                                previousFromYear,
                                                safeToYear)
                                : researchPaperRepository.getTopicComparisonTrends(
                                                new ArrayList<>(requestedNames.keySet()),
                                                previousFromYear,
                                                safeToYear);

                Map<String, Map<Integer, Long>> countsByNameAndYear = new LinkedHashMap<>();
                requestedNames.keySet().forEach(name -> countsByNameAndYear.put(name, new LinkedHashMap<>()));
                for (Object[] row : rows) {
                        String normalizedName = (String) row[0];
                        countsByNameAndYear.get(normalizedName).put(
                                        (Integer) row[1],
                                        ((Number) row[2]).longValue());
                }

                List<String> missingNames = requestedNames.keySet().stream()
                                .filter(name -> countsByNameAndYear.get(name).isEmpty())
                                .map(requestedNames::get)
                                .toList();
                if (!missingNames.isEmpty()) {
                        throw new BadRequestException(
                                        "Không tìm thấy dữ liệu " + safeType.toLowerCase()
                                                        + " cho: " + String.join(", ", missingNames));
                }

                List<TrendComparisonSeriesResponse> series = requestedNames.entrySet().stream()
                                .map(entry -> toComparisonSeries(
                                                entry.getValue(),
                                                countsByNameAndYear.get(entry.getKey()),
                                                safeFromYear,
                                                safeToYear))
                                .toList();

                return new TrendComparisonResponse(
                                safeType,
                                safeFromYear,
                                safeToYear,
                                series);
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
                long totalPapers = recentCount + previousCount;
                if (totalPapers < Math.max(0, minPapersThreshold)) {
                        return TREND_TYPE_INSUFFICIENT_DATA;
                }

                double growthRate = calculateGrowthRate(recentCount, previousCount);
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

                if (growthRate < -Math.abs(stableGrowthRate)) {
                        return TREND_TYPE_DECLINING;
                }

                if (Math.abs(growthRate) <= Math.abs(stableGrowthRate)) {
                        return TREND_TYPE_STABLE;
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

        private void validateComparisonYears(
                        int fromYear,
                        int toYear,
                        int currentYear) {
                if (fromYear > toYear) {
                        throw new BadRequestException("fromYear không được lớn hơn toYear");
                }
                if (fromYear < 1900 || toYear > currentYear) {
                        throw new BadRequestException(
                                        "Khoảng năm phải nằm trong khoảng từ 1900 đến năm hiện tại");
                }
        }

        private void validatePreviousPeriod(int previousFromYear) {
                if (previousFromYear < 1900) {
                        throw new BadRequestException(
                                        "Khoảng năm không đủ dữ liệu để tạo giai đoạn so sánh liền trước");
                }
        }

        private LinkedHashMap<String, String> normalizeComparisonNames(List<String> itemNames) {
                if (itemNames == null) {
                        throw new BadRequestException("Cần chọn từ 2 đến 5 keyword hoặc topic để so sánh");
                }

                LinkedHashMap<String, String> requestedNames = new LinkedHashMap<>();
                itemNames.stream()
                                .filter(name -> name != null && !name.isBlank())
                                .forEach(name -> requestedNames.putIfAbsent(
                                                normalizeKeyword(name),
                                                name.trim()));

                if (requestedNames.size() < MIN_COMPARE_ITEMS
                                || requestedNames.size() > MAX_COMPARE_ITEMS) {
                        throw new BadRequestException(
                                        "Cần chọn từ 2 đến 5 keyword hoặc topic khác nhau để so sánh");
                }

                return requestedNames;
        }

        private String normalizeComparisonType(String type) {
                if (type == null || type.isBlank()) {
                        throw new BadRequestException("type phải là KEYWORD hoặc TOPIC");
                }

                String safeType = type.trim().toUpperCase();
                if (!"KEYWORD".equals(safeType) && !"TOPIC".equals(safeType)) {
                        throw new BadRequestException("type phải là KEYWORD hoặc TOPIC");
                }

                return safeType;
        }

        private TrendComparisonSeriesResponse toComparisonSeries(
                        String name,
                        Map<Integer, Long> yearlyCounts,
                        int fromYear,
                        int toYear) {
                List<TrendResponse> yearlyData = new ArrayList<>();
                int periodYears = toYear - fromYear + 1;
                int previousFromYear = fromYear - periodYears;
                long previousCount = 0;
                long recentCount = 0;
                for (int year = fromYear; year <= toYear; year++) {
                        long paperCount = yearlyCounts.getOrDefault(year, 0L);
                        recentCount += paperCount;
                        yearlyData.add(new TrendResponse(year, paperCount));
                }

                for (int year = previousFromYear; year < fromYear; year++) {
                        previousCount += yearlyCounts.getOrDefault(year, 0L);
                }

                long totalPapers = previousCount + recentCount;
                TrendComparisonSeriesResponse response = new TrendComparisonSeriesResponse(
                                name,
                                totalPapers,
                                calculateGrowthRate(recentCount, previousCount),
                                yearlyData);
                response.setPreviousCount(previousCount);
                response.setRecentCount(recentCount);
                response.setTrendScore(calculateTrendScore(response.getGrowthRate(), totalPapers));
                response.setTrendType(resolveTrendType(recentCount, previousCount));
                response.setSufficientData(totalPapers >= Math.max(0, minPapersThreshold));
                return response;
        }

        private TrendAnalysisResponse analyzeSingleTrend(
                        String type,
                        String name,
                        List<TrendResponse> allYearlyData,
                        Integer recentFromYear,
                        Integer recentToYear) {
                if (name == null || name.isBlank()) {
                        throw new BadRequestException(type.toLowerCase() + " không được để trống");
                }

                int currentYear = Year.now().getValue();
                int safeRecentToYear = recentToYear == null ? currentYear : recentToYear;
                int safeRecentFromYear = recentFromYear == null
                                ? safeRecentToYear - DEFAULT_TREND_PERIOD_YEARS + 1
                                : recentFromYear;
                validateComparisonYears(safeRecentFromYear, safeRecentToYear, currentYear);

                int periodYears = safeRecentToYear - safeRecentFromYear + 1;
                int previousFromYear = safeRecentFromYear - periodYears;
                int previousToYear = safeRecentFromYear - 1;
                validatePreviousPeriod(previousFromYear);
                Map<Integer, Long> countsByYear = allYearlyData.stream()
                                .collect(Collectors.toMap(
                                                TrendResponse::getYear,
                                                TrendResponse::getPaperCount,
                                                Long::sum));

                long previousCount = sumCounts(countsByYear, previousFromYear, previousToYear);
                long recentCount = sumCounts(countsByYear, safeRecentFromYear, safeRecentToYear);
                long totalPapers = previousCount + recentCount;
                double growthRate = calculateGrowthRate(recentCount, previousCount);
                List<TrendResponse> yearlyData = buildYearlyData(
                                countsByYear,
                                safeRecentFromYear,
                                safeRecentToYear);

                return new TrendAnalysisResponse(
                                type,
                                name.trim(),
                                previousFromYear,
                                previousToYear,
                                safeRecentFromYear,
                                safeRecentToYear,
                                previousCount,
                                recentCount,
                                totalPapers,
                                growthRate,
                                calculateTrendScore(growthRate, totalPapers),
                                resolveTrendType(recentCount, previousCount),
                                totalPapers >= Math.max(0, minPapersThreshold),
                                yearlyData);
        }

        private long sumCounts(Map<Integer, Long> countsByYear, int fromYear, int toYear) {
                long total = 0;
                for (int year = fromYear; year <= toYear; year++) {
                        total += countsByYear.getOrDefault(year, 0L);
                }
                return total;
        }

        private List<TrendResponse> buildYearlyData(
                        Map<Integer, Long> countsByYear,
                        int fromYear,
                        int toYear) {
                List<TrendResponse> yearlyData = new ArrayList<>();
                for (int year = fromYear; year <= toYear; year++) {
                        yearlyData.add(new TrendResponse(year, countsByYear.getOrDefault(year, 0L)));
                }
                return yearlyData;
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
