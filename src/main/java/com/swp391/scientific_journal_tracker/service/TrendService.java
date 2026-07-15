package com.swp391.scientific_journal_tracker.service;

import java.time.Year;
import java.util.List;

import org.springframework.data.domain.PageRequest;
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
         * Lấy những topic có tổng số lượng paper cao nhất từ một năm cụ thể.
         *
         * Limit được giới hạn từ 1 đến 20 để tránh trả về quá nhiều dữ liệu.
         * Nếu fromYear không được truyền vào, hệ thống dùng 5 năm gần nhất.
         *
         * @param fromYear năm bắt đầu thống kê
         * @param limit    số lượng topic tối đa
         * @return danh sách topic và tổng số paper
         */
        @Transactional(readOnly = true)
        public List<TopTopicResponse> getTopTrendingTopics(
                        Integer fromYear,
                        int limit) {
                int safeLimit = Math.max(
                                1,
                                Math.min(limit, 20));

                if (fromYear == null) {
                        fromYear = Year.now().getValue() - 5;
                }

                return researchPaperRepository
                                .getTop5TrendingTopics(
                                                fromYear,
                                                PageRequest.of(0, safeLimit))
                                .stream()
                                .map(row -> new TopTopicResponse(
                                                (String) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }
}