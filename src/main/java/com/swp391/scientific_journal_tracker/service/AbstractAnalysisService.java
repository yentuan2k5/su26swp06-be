package com.swp391.scientific_journal_tracker.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.swp391.scientific_journal_tracker.dto.response.AbstractAnalysisResponse;

/**
 * Trích xuất các câu objective, problem, method và result từ abstract bằng
 * các cụm từ học thuật phổ biến. Kết quả chỉ là gợi ý đọc nhanh cho chức năng
 * so sánh paper, không thay thế việc đánh giá nội dung nghiên cứu.
 */
@Service
public class AbstractAnalysisService {

    private static final String ABSTRACT_AVAILABLE = "ABSTRACT_AVAILABLE";
    private static final String ABSTRACT_NOT_AVAILABLE = "ABSTRACT_NOT_AVAILABLE";
    private static final int MAX_ANALYZED_CHARACTERS = 20_000;
    private static final int MAX_HIGHLIGHTS_PER_SECTION = 2;
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    private static final List<Pattern> OBJECTIVE_INDICATORS = List.of(
            Pattern.compile("\\bthis (paper|study|work) (aims to|aims at|investigates|examines|explores|focuses on)\\b"),
            Pattern.compile("\\bwe (aim to|investigate|examine|explore)\\b"),
            Pattern.compile("\\bthe objective of (this|the) (paper|study|work)\\b"));

    private static final List<Pattern> PROBLEM_INDICATORS = List.of(
            Pattern.compile("\\b(challenge|limitation|gap|lack of|scarce|difficult|underexplored)\\b"),
            Pattern.compile("\\b(problem|issue) of\\b"),
            Pattern.compile("\\b(remains|remain) (a )?(challenge|unclear|limited)\\b"),
            Pattern.compile("\\bthis work addresses\\b"));

    private static final List<Pattern> METHOD_INDICATORS = List.of(
            Pattern.compile("\\bwe (propose|introduce|develop|employ|present|design)\\b"),
            Pattern.compile("\\b(the )?proposed (method|model|approach|framework|architecture|system)\\b"),
            Pattern.compile("\\ba novel (method|model|approach|framework|architecture|system)\\b"));

    private static final List<Pattern> RESULT_INDICATORS = List.of(
            Pattern.compile("\\b(results?|findings?|experiments?|evaluation) (show|shows|demonstrate|demonstrates|indicate|indicates|reveal|reveals|confirm|confirms)\\b"),
            Pattern.compile("\\b(outperform|outperforms|outperformed|achieve|achieves|achieved|improve|improves|improved)\\b"),
            Pattern.compile("\\bempirical (results?|analysis|evaluation)\\b"));

    public AbstractAnalysisResponse analyze(String abstractText) {
        if (abstractText == null || abstractText.isBlank()) {
            return new AbstractAnalysisResponse(
                    ABSTRACT_NOT_AVAILABLE,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
        }

        List<String> sentences = splitSentences(abstractText);
        return new AbstractAnalysisResponse(
                ABSTRACT_AVAILABLE,
                extractHighlights(sentences, OBJECTIVE_INDICATORS),
                extractHighlights(sentences, PROBLEM_INDICATORS),
                extractHighlights(sentences, METHOD_INDICATORS),
                extractHighlights(sentences, RESULT_INDICATORS));
    }

    private List<String> splitSentences(String abstractText) {
        String normalizedAbstract = abstractText
                .replaceAll("\\s+", " ")
                .trim();

        if (normalizedAbstract.length() > MAX_ANALYZED_CHARACTERS) {
            normalizedAbstract = normalizedAbstract.substring(0, MAX_ANALYZED_CHARACTERS);
        }

        return Arrays.stream(SENTENCE_BOUNDARY.split(normalizedAbstract))
                .map(String::trim)
                .filter(sentence -> !sentence.isBlank())
                .toList();
    }

    private List<String> extractHighlights(List<String> sentences, List<Pattern> indicators) {
        return sentences.stream()
                .filter(sentence -> containsIndicator(sentence, indicators))
                .limit(MAX_HIGHLIGHTS_PER_SECTION)
                .toList();
    }

    private boolean containsIndicator(String sentence, List<Pattern> indicators) {
        String normalizedSentence = sentence.toLowerCase(Locale.ROOT);
        return indicators.stream()
                .anyMatch(indicator -> indicator.matcher(normalizedSentence).find());
    }
}
