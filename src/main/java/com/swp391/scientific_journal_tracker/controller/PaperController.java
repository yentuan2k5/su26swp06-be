package com.swp391.scientific_journal_tracker.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swp391.scientific_journal_tracker.dto.response.PaperResponse;
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
     * Query params (tất cả đều optional):
     *   search   - tìm trong title / abstract / authors
     *   author   - lọc theo tên tác giả (contains)
     *   keyword  - lọc theo keyword chính xác
     *   journal  - lọc theo tên journal (contains)
     *   topic    - lọc theo tên topic (contains)
     *   yearFrom - năm bắt đầu (>= yearFrom)
     *   yearTo   - năm kết thúc (<= yearTo)
     *   page     - trang (mặc định 0)
     *   size     - số kết quả mỗi trang (mặc định 10)
     */
    @GetMapping
    public Page<PaperResponse> getPapers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String journal,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return paperService.getPapers(search, author, keyword, journal, topic, yearFrom, yearTo, page, size);
    }

    @GetMapping("/{id}")
    public PaperResponse getPaperById(@PathVariable Long id) {
        return paperService.getPaperById(id);
    }
}
