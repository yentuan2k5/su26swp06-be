package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        private static final int DEFAULT_TOPIC_TREND_PERIOD_YEARS = 5;

        @Value("${trend.min-papers-threshold:30}")
        private int minPapersThreshold;

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
         * Lấy những topic đang tăng trưởng mạnh trong giai đoạn gần đây.
         *
         * Limit được giới hạn từ 1 đến 20 để tránh trả về quá nhiều dữ liệu.
         * Nếu fromYear không được truyền vào, hệ thống dùng 5 năm gần nhất
         * làm recentPeriod và lấy thêm một giai đoạn liền trước có cùng độ dài
         * để so sánh.
         *
         * Công thức growth rate:
         * growthRate = (recentCount - previousCount) / previousCount.
         * Nếu previousCount = 0, hệ thống coi topic tăng 100% khi recentCount > 0
         * để tránh chia cho 0.
         *
         * minPapersThreshold loại các topic có tổng số paper quá nhỏ trong cả
         * hai giai đoạn. Việc này giúp tránh nhiễu thống kê, ví dụ topic tăng
         * từ 2 lên 5 paper nhìn có vẻ tăng rất mạnh nhưng chưa đủ dữ liệu để
         * xem là xu hướng đáng tin.
         *
         * Sau khi qua ngưỡng tối thiểu, topic được xếp hạng bằng điểm:
         * score = growthRate * log(1 + totalPapers).
         * Công thức này giữ trọng tâm là tốc độ tăng trưởng, nhưng cộng thêm
         * sức nặng vừa phải cho các topic có volume lớn hơn.
         *
         * @param fromYear năm bắt đầu thống kê
         * @param limit    số lượng topic tối đa
         * @return danh sách topic, số paper gần đây, growth rate, tổng paper và score
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
                                ? currentYear - DEFAULT_TOPIC_TREND_PERIOD_YEARS + 1
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
                                score);
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
