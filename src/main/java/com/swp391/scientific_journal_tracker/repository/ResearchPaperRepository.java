package com.swp391.scientific_journal_tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swp391.scientific_journal_tracker.entity.ResearchPaper;

public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, Long> {

    List<ResearchPaper> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    Optional<ResearchPaper> findByDoi(String doi);

    boolean existsByDoi(String doi);

    List<ResearchPaper> findByTitleContainingIgnoreCase(String title);

    List<ResearchPaper> findDistinctByAuthors_FullNameContainingIgnoreCase(
            String author);

    List<ResearchPaper> findByYear(Integer year);

    List<ResearchPaper> findBySourceApi(String sourceApi);

    List<ResearchPaper> findByJournalJournalId(Long journalId);

    Page<ResearchPaper> findByJournalJournalId(Long journalId, Pageable pageable);

    List<ResearchPaper> findByApiDataSourceApiDataSourceId(Long apiDataSourceId);

    List<ResearchPaper> findTop10ByOrderByCitationCountDesc();

    List<ResearchPaper> findTop10ByOrderByYearDesc();

    long countBySourceApi(String sourceApi);

    @Query("""
                SELECT p.year, COUNT(p)
                FROM ResearchPaper p
                WHERE p.year IS NOT NULL
                GROUP BY p.year
                ORDER BY p.year DESC
            """)
    List<Object[]> countPapersByYear();

    @Query("""
                SELECT k.term, COUNT(p)
                FROM ResearchPaper p
                JOIN p.keywords k
                GROUP BY k.term
                ORDER BY COUNT(p) DESC
            """)
    List<Object[]> countTopKeywords(Pageable pageable);

    @Query("""
                SELECT j.title, COUNT(p)
                FROM ResearchPaper p
                JOIN p.journal j
                GROUP BY j.title
                ORDER BY COUNT(p) DESC
            """)
    List<Object[]> countTopJournals(Pageable pageable);

    @Query("""
            SELECT COUNT(p)
            FROM ResearchPaper p
            WHERE (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            """)
    long countReportPapers(
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic);

    @Query("""
            SELECT COUNT(p)
            FROM ResearchPaper p
            WHERE p.sourceApi = :sourceApi
            AND (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            """)
    long countReportPapersBySource(
            @Param("sourceApi") String sourceApi,
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic);

    @Query("""
            SELECT COUNT(DISTINCT j)
            FROM ResearchPaper p
            JOIN p.journal j
            WHERE (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            """)
    long countReportJournals(
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic);

    @Query("""
            SELECT COUNT(DISTINCT k)
            FROM ResearchPaper p
            JOIN p.keywords k
            WHERE (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            """)
    long countReportKeywords(
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic);

    @Query("""
            SELECT p.year, COUNT(DISTINCT p)
            FROM ResearchPaper p
            WHERE p.year IS NOT NULL
            AND (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            GROUP BY p.year
            ORDER BY p.year ASC
            """)
    List<Object[]> countReportPapersByYear(
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic);

    @Query("""
            SELECT k.term, COUNT(DISTINCT p)
            FROM ResearchPaper p
            JOIN p.keywords k
            WHERE (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            GROUP BY k.keywordId, k.term
            ORDER BY COUNT(DISTINCT p) DESC
            """)
    List<Object[]> countReportTopKeywords(
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic,
            Pageable pageable);

    @Query("""
            SELECT j.title, COUNT(DISTINCT p)
            FROM ResearchPaper p
            JOIN p.journal j
            WHERE (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            GROUP BY j.journalId, j.title
            ORDER BY COUNT(DISTINCT p) DESC
            """)
    List<Object[]> countReportTopJournals(
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic,
            Pageable pageable);

    @Query("""
            SELECT p
            FROM ResearchPaper p
            WHERE (:fromYear IS NULL OR p.year >= :fromYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            ORDER BY p.citationCount DESC
            """)
    List<ResearchPaper> findReportTopCitedPapers(
            @Param("fromYear") Integer fromYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT p
            FROM ResearchPaper p
            LEFT JOIN p.keywords k
            LEFT JOIN p.authors a
            WHERE (
                :search IS NULL
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:year IS NULL OR p.year = :year)
            AND (
                :keyword IS NULL
                OR LOWER(k.term) = LOWER(:keyword)
            )
            """)
    Page<ResearchPaper> searchPapers(
            @Param("search") String search,
            @Param("year") Integer year,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT p
            FROM ResearchPaper p
            WHERE (:year IS NULL OR p.year = :year)
            AND (:yearFrom IS NULL OR p.year >= :yearFrom)
            AND (:yearTo IS NULL OR p.year <= :yearTo)
            """, countQuery = """
            SELECT COUNT(p)
            FROM ResearchPaper p
            WHERE (:year IS NULL OR p.year = :year)
            AND (:yearFrom IS NULL OR p.year >= :yearFrom)
            AND (:yearTo IS NULL OR p.year <= :yearTo)
            """)
    Page<ResearchPaper> searchPapersByYearRange(
            @Param("year") Integer year,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            Pageable pageable);

    /*
     * LƯU Ý HIỆU NĂNG:
     * Trước đây query này LEFT JOIN cùng lúc 4 quan hệ many-to-many/many-to-one
     * độc lập (keywords, researchTopics, journal, authors). Khi join nhiều
     * collection độc lập trong 1 câu, MySQL nhân số dòng theo tích của chúng
     * (fan-out) — 1 paper có 10 keyword x 5 topic x N author sẽ sinh ra tới
     * hàng trăm dòng trung gian TRƯỚC KHI DISTINCT lọc lại. Với vài nghìn
     * paper, tổng số dòng trung gian có thể lên tới hàng triệu, khiến query
     * (và cả countQuery chạy song song cho phân trang) rất chậm, dễ gây
     * request timeout — đặc biệt khi kết hợp với LIKE '%...%' (wildcard đầu,
     * không dùng được index) và Hikari pool chỉ có 5 connection.
     *
     * Cách sửa: thay LEFT JOIN các collection many-to-many bằng EXISTS
     * subquery (giống pattern đã dùng ở countReportPapers phía trên). EXISTS
     * chỉ kiểm tra "có tồn tại hay không" cho từng paper, không nhân dòng,
     * nên tránh hoàn toàn hiện tượng fan-out. Chỉ giữ LEFT JOIN cho
     * p.journal vì đây là quan hệ many-to-one (1 paper chỉ có 1 journal,
     * không gây nhân dòng).
     */
    @Query(value = """
            SELECT DISTINCT p
            FROM ResearchPaper p
            LEFT JOIN p.journal j
            WHERE
                (
                    :search IS NULL
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR EXISTS (
                        SELECT 1 FROM p.authors sa
                        WHERE LOWER(sa.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                    OR EXISTS (
                        SELECT 1 FROM p.keywords sk
                        WHERE LOWER(sk.term) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                    OR EXISTS (
                        SELECT 1 FROM p.researchTopics st
                        WHERE LOWER(st.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                )
                AND (
                    :author IS NULL
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :author, '%'))
                    OR EXISTS (
                        SELECT 1 FROM p.authors aa
                        WHERE LOWER(aa.fullName) LIKE LOWER(CONCAT('%', :author, '%'))
                    )
                )
                AND (
                    :keyword IS NULL
                    OR EXISTS (
                        SELECT 1 FROM p.keywords kk
                        WHERE LOWER(kk.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                )
                AND (
                    :journal IS NULL
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :journal, '%'))
                )
                AND (
                    :topic IS NULL
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR EXISTS (
                        SELECT 1 FROM p.researchTopics tt
                        WHERE LOWER(tt.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                    )
                    OR EXISTS (
                        SELECT 1 FROM p.keywords tk
                        WHERE LOWER(tk.term) LIKE LOWER(CONCAT('%', :topic, '%'))
                    )
                )
                AND (:year IS NULL OR p.year = :year)
                AND (:yearFrom IS NULL OR p.year >= :yearFrom)
                AND (:yearTo IS NULL OR p.year <= :yearTo)
            """, countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM ResearchPaper p
            LEFT JOIN p.journal j
            WHERE
                (
                    :search IS NULL
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR EXISTS (
                        SELECT 1 FROM p.authors sa
                        WHERE LOWER(sa.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                    OR EXISTS (
                        SELECT 1 FROM p.keywords sk
                        WHERE LOWER(sk.term) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                    OR EXISTS (
                        SELECT 1 FROM p.researchTopics st
                        WHERE LOWER(st.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                )
                AND (
                    :author IS NULL
                    OR LOWER(p.authorsRaw) LIKE LOWER(CONCAT('%', :author, '%'))
                    OR EXISTS (
                        SELECT 1 FROM p.authors aa
                        WHERE LOWER(aa.fullName) LIKE LOWER(CONCAT('%', :author, '%'))
                    )
                )
                AND (
                    :keyword IS NULL
                    OR EXISTS (
                        SELECT 1 FROM p.keywords kk
                        WHERE LOWER(kk.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
                )
                AND (
                    :journal IS NULL
                    OR LOWER(j.title) LIKE LOWER(CONCAT('%', :journal, '%'))
                )
                AND (
                    :topic IS NULL
                    OR LOWER(p.title) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :topic, '%'))
                    OR EXISTS (
                        SELECT 1 FROM p.researchTopics tt
                        WHERE LOWER(tt.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                    )
                    OR EXISTS (
                        SELECT 1 FROM p.keywords tk
                        WHERE LOWER(tk.term) LIKE LOWER(CONCAT('%', :topic, '%'))
                    )
                )
                AND (:year IS NULL OR p.year = :year)
                AND (:yearFrom IS NULL OR p.year >= :yearFrom)
                AND (:yearTo IS NULL OR p.year <= :yearTo)
            """)
    Page<ResearchPaper> searchPapersAdvanced(
            @Param("search") String search,
            @Param("author") String author,
            @Param("keyword") String keyword,
            @Param("journal") String journal,
            @Param("topic") String topic,
            @Param("year") Integer year,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            Pageable pageable);

    @Query("""
                SELECT p.year, COUNT(DISTINCT p)
                FROM ResearchPaper p
                JOIN p.keywords k
                WHERE p.year IS NOT NULL
                AND LOWER(k.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                GROUP BY p.year
                ORDER BY p.year ASC
            """)
    List<Object[]> getTrendByKeyword(@Param("keyword") String keyword);

    @Query("""
                SELECT p.year, COUNT(DISTINCT p)
                FROM ResearchPaper p
                JOIN p.researchTopics t
                WHERE p.year IS NOT NULL
                AND LOWER(t.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                GROUP BY p.year
                ORDER BY p.year ASC
            """)
    List<Object[]> getTrendByTopic(@Param("topic") String topic);

    /**
     * Đếm số lượng paper theo từng năm thuộc một lĩnh vực cụ thể.
     *
     * Query đi từ ResearchPaper sang Journal vì field đang được lưu trong
     * Journal.field, không nằm trực tiếp trong ResearchPaper.
     *
     * LEFT JOIN được sử dụng vì một số paper có thể chưa được gắn journal.
     * Những paper không có journal hoặc không có field sẽ được loại khỏi
     * kết quả bằng điều kiện j.field IS NOT NULL.
     *
     * COUNT(DISTINCT p) tránh trường hợp một paper bị đếm trùng.
     *
     * @param field tên lĩnh vực cần phân tích, ví dụ "Computer Science"
     * @return danh sách Object[] gồm [year, paperCount], sắp xếp năm tăng dần
     */
    @Query("""
            SELECT p.year, COUNT(DISTINCT p)
            FROM ResearchPaper p
            LEFT JOIN p.journal j
            WHERE p.year IS NOT NULL
            AND j.field IS NOT NULL
            AND TRIM(j.field) <> ''
            AND LOWER(j.field) LIKE LOWER(CONCAT('%', :field, '%'))
            GROUP BY p.year
            ORDER BY p.year ASC
            """)
    List<Object[]> getTrendByField(
            @Param("field") String field);

    /**
     * Thống kê số paper của từng topic trong hai giai đoạn liên tiếp.
     *
     * recentPeriod bắt đầu từ recentStartYear và có cùng độ dài với
     * previousPeriod. Vì vậy năm kết thúc của recentPeriod được suy ra từ
     * recentStartYear + previousEndYear - previousStartYear.
     *
     * SUM(CASE WHEN ...) giúp mỗi topic chỉ xuất hiện một dòng kết quả,
     * gồm cả số paper của giai đoạn gần đây và giai đoạn liền trước.
     *
     * @param recentStartYear   năm bắt đầu giai đoạn gần đây
     * @param previousStartYear năm bắt đầu giai đoạn liền trước
     * @param previousEndYear   năm kết thúc giai đoạn liền trước
     * @return danh sách Object[] gồm [topicName, recentCount, previousCount]
     */
    @Query("""
            SELECT t.name,
                   SUM(CASE
                           WHEN p.year BETWEEN :recentStartYear
                               AND (:recentStartYear + :previousEndYear - :previousStartYear)
                           THEN 1
                           ELSE 0
                       END),
                   SUM(CASE
                           WHEN p.year BETWEEN :previousStartYear AND :previousEndYear
                           THEN 1
                           ELSE 0
                       END)
            FROM ResearchPaper p
            JOIN p.researchTopics t
            WHERE p.year IS NOT NULL
            AND p.year BETWEEN :previousStartYear
                AND (:recentStartYear + :previousEndYear - :previousStartYear)
            GROUP BY t.researchTopicId, t.name
            """)
    List<Object[]> getTopicGrowthStats(
            @Param("recentStartYear") int recentStartYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear);

    /**
     * Thống kê số paper của từng keyword trong hai giai đoạn liên tiếp.
     *
     * Query này dùng cùng nguyên tắc với topic growth stats để phục vụ
     * bảng top trending keywords. Mỗi keyword chỉ xuất hiện một dòng,
     * gồm số paper của giai đoạn gần đây và giai đoạn liền trước.
     *
     * @param recentStartYear   năm bắt đầu giai đoạn gần đây
     * @param previousStartYear năm bắt đầu giai đoạn liền trước
     * @param previousEndYear   năm kết thúc giai đoạn liền trước
     * @return danh sách Object[] gồm [keywordTerm, recentCount, previousCount]
     */
    @Query("""
            SELECT k.term,
                   SUM(CASE
                           WHEN p.year BETWEEN :recentStartYear
                               AND (:recentStartYear + :previousEndYear - :previousStartYear)
                           THEN 1
                           ELSE 0
                       END),
                   SUM(CASE
                           WHEN p.year BETWEEN :previousStartYear AND :previousEndYear
                           THEN 1
                           ELSE 0
                       END)
            FROM ResearchPaper p
            JOIN p.keywords k
            WHERE p.year IS NOT NULL
            AND k.term IS NOT NULL
            AND TRIM(k.term) <> ''
            AND p.year BETWEEN :previousStartYear
                AND (:recentStartYear + :previousEndYear - :previousStartYear)
            GROUP BY k.keywordId, k.term
            """)
    List<Object[]> getKeywordGrowthStats(
            @Param("recentStartYear") int recentStartYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear);

    @Query("""
            SELECT t.name,
                   SUM(CASE
                           WHEN p.year BETWEEN :recentStartYear
                               AND (:recentStartYear + :previousEndYear - :previousStartYear)
                           THEN 1
                           ELSE 0
                       END),
                   SUM(CASE
                           WHEN p.year BETWEEN :previousStartYear AND :previousEndYear
                           THEN 1
                           ELSE 0
                       END)
            FROM ResearchPaper p
            JOIN p.researchTopics t
            WHERE p.year IS NOT NULL
            AND p.year BETWEEN :previousStartYear
                AND (:recentStartYear + :previousEndYear - :previousStartYear)
            AND (
                :keyword IS NULL
                OR EXISTS (
                    SELECT keywordFilter.keywordId
                    FROM p.keywords keywordFilter
                    WHERE LOWER(keywordFilter.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            AND (
                :topic IS NULL
                OR EXISTS (
                    SELECT topicFilter.researchTopicId
                    FROM p.researchTopics topicFilter
                    WHERE LOWER(topicFilter.name) LIKE LOWER(CONCAT('%', :topic, '%'))
                )
            )
            GROUP BY t.researchTopicId, t.name
            """)
    List<Object[]> getReportTopicGrowthStats(
            @Param("recentStartYear") int recentStartYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear,
            @Param("keyword") String keyword,
            @Param("topic") String topic);

    @Query("""
                SELECT t.name, COUNT(DISTINCT p)
                FROM ResearchPaper p
                JOIN p.researchTopics t
                WHERE p.year IS NOT NULL
                AND p.year >= :fromYear
                GROUP BY t.researchTopicId, t.name
                ORDER BY COUNT(DISTINCT p) DESC
            """)
    List<Object[]> getTop5TrendingTopics(
            @Param("fromYear") Integer fromYear,
            Pageable pageable);

    /** Thống kê node gốc Keyword theo tổng paper và hai giai đoạn xu hướng. */
    @Query("""
            SELECT COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.keywords k
            WHERE k.keywordId = :keywordId
            """)
    Object[] getKeywordMindMapStats(
            @Param("keywordId") Long keywordId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear);

    /** Thống kê node gốc Topic theo tổng paper và hai giai đoạn xu hướng. */
    @Query("""
            SELECT COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.researchTopics t
            WHERE t.researchTopicId = :topicId
            """)
    Object[] getTopicMindMapStats(
            @Param("topicId") Long topicId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear);

    @Query("""
            SELECT t.researchTopicId, t.name, COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.keywords rootKeyword
            JOIN p.researchTopics t
            WHERE rootKeyword.keywordId = :keywordId
            GROUP BY t.researchTopicId, t.name
            ORDER BY COUNT(DISTINCT p) DESC, t.name ASC
            """)
    List<Object[]> findMindMapTopicsForKeyword(
            @Param("keywordId") Long keywordId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear,
            Pageable pageable);

    @Query("""
            SELECT relatedKeyword.keywordId, relatedKeyword.term, COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.keywords rootKeyword
            JOIN p.keywords relatedKeyword
            WHERE rootKeyword.keywordId = :keywordId
            AND relatedKeyword.keywordId <> :keywordId
            GROUP BY relatedKeyword.keywordId, relatedKeyword.term
            ORDER BY COUNT(DISTINCT p) DESC, relatedKeyword.term ASC
            """)
    List<Object[]> findMindMapKeywordsForKeyword(
            @Param("keywordId") Long keywordId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear,
            Pageable pageable);

    @Query("""
            SELECT j.journalId, j.title, COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.keywords rootKeyword
            JOIN p.journal j
            WHERE rootKeyword.keywordId = :keywordId
            GROUP BY j.journalId, j.title
            ORDER BY COUNT(DISTINCT p) DESC, j.title ASC
            """)
    List<Object[]> findMindMapJournalsForKeyword(
            @Param("keywordId") Long keywordId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear,
            Pageable pageable);

    @Query("""
            SELECT relatedTopic.researchTopicId, relatedTopic.name, COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.researchTopics rootTopic
            JOIN p.researchTopics relatedTopic
            WHERE rootTopic.researchTopicId = :topicId
            AND relatedTopic.researchTopicId <> :topicId
            GROUP BY relatedTopic.researchTopicId, relatedTopic.name
            ORDER BY COUNT(DISTINCT p) DESC, relatedTopic.name ASC
            """)
    List<Object[]> findMindMapTopicsForTopic(
            @Param("topicId") Long topicId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear,
            Pageable pageable);

    @Query("""
            SELECT k.keywordId, k.term, COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.researchTopics rootTopic
            JOIN p.keywords k
            WHERE rootTopic.researchTopicId = :topicId
            GROUP BY k.keywordId, k.term
            ORDER BY COUNT(DISTINCT p) DESC, k.term ASC
            """)
    List<Object[]> findMindMapKeywordsForTopic(
            @Param("topicId") Long topicId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear,
            Pageable pageable);

    @Query("""
            SELECT j.journalId, j.title, COUNT(DISTINCT p),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :recentStartYear AND :currentYear THEN 1
                       ELSE 0
                   END), 0),
                   COALESCE(SUM(CASE
                       WHEN p.year BETWEEN :previousStartYear AND :previousEndYear THEN 1
                       ELSE 0
                   END), 0)
            FROM ResearchPaper p
            JOIN p.researchTopics rootTopic
            JOIN p.journal j
            WHERE rootTopic.researchTopicId = :topicId
            GROUP BY j.journalId, j.title
            ORDER BY COUNT(DISTINCT p) DESC, j.title ASC
            """)
    List<Object[]> findMindMapJournalsForTopic(
            @Param("topicId") Long topicId,
            @Param("recentStartYear") int recentStartYear,
            @Param("currentYear") int currentYear,
            @Param("previousStartYear") int previousStartYear,
            @Param("previousEndYear") int previousEndYear,
            Pageable pageable);
}
