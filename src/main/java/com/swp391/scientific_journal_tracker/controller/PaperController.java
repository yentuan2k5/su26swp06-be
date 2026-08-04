package com.swp391.scientific_journal_tracker.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
import com.swp391.scientific_journal_tracker.dto.response.PaperComparisonResponse;
import com.swp391.scientific_journal_tracker.service.PaperService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    /**
     * GET /api/papers
     *
     * Query params:
     * search - tìm chung trong title / abstract / authors / keyword / journal /
     * topic
     * author - lọc theo tác giả
     * keyword - lọc theo keyword
     * journal - lọc theo journal
     * topic - lọc theo topic
     * year - lọc đúng một năm
     * yearFrom - lọc từ năm
     * yearTo - lọc đến năm
     * page - số trang, mặc định 0
     * size - số kết quả, mặc định 10, tối đa 50
     * sortBy - year / citationCount / title / researchPaperId
     * sortDir - asc / desc
     */
    @GetMapping
    public Page<PaperResponse> getPapers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String journal,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return paperService.getPapers(
                search,
                author,
                keyword,
                journal,
                topic,
                year,
                yearFrom,
                yearTo,
                page,
                size,
                sortBy,
                sortDir);
    }

    /**
     * So sánh metadata của từ hai đến bốn paper.
     *
     * Ví dụ:
     * GET /api/papers/compare?ids=120&ids=121&ids=122
     *
     * Response gồm metadata từng paper, keyword/topic chung và riêng, cùng độ
     * tương đồng của từng cặp paper. Không xử lý toàn văn PDF và không ghi dữ
     * liệu xuống database.
     *
     * @param ids từ 2 đến 4 ResearchPaperId khác nhau
     * @return dữ liệu cho bảng và ma trận so sánh ở frontend
     */
    @GetMapping("/compare")
    public PaperComparisonResponse comparePapers(@RequestParam List<Long> ids) {
        return paperService.comparePapers(ids);
    }

    @GetMapping("/{id}")
    public PaperResponse getPaperById(@PathVariable Long id) {
        return paperService.getPaperById(id);
    }
}
