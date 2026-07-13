package com.swp391.scientific_journal_tracker.dto.response;

import java.util.Arrays;
import java.util.List;

import com.swp391.scientific_journal_tracker.entity.Author;
import com.swp391.scientific_journal_tracker.entity.Keyword;
import com.swp391.scientific_journal_tracker.entity.ResearchPaper;
import com.swp391.scientific_journal_tracker.entity.ResearchTopic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperResponse {

        private Long researchPaperId;
        private String externalId;
        private String title;
        private String abstractText;
        private Integer year;
        private String doi;
        private Integer citationCount;
        private String sourceApi;

        private List<String> authors;

        private List<String> keywords;
        private String journalTitle;
        private List<String> topics;

        public static PaperResponse fromEntity(ResearchPaper paper) {

                /*
                 * Ưu tiên lấy tác giả từ quan hệ ManyToMany.
                 */
                List<String> authors = paper.getAuthors() == null
                                ? List.of()
                                : paper.getAuthors()
                                                .stream()
                                                .map(Author::getFullName)
                                                .filter(name -> name != null && !name.isBlank())
                                                .distinct()
                                                .toList();

                /*
                 * Fallback cho dữ liệu cũ:
                 * Nếu paper_authors chưa có dữ liệu thì lấy từ cột Authors cũ.
                 */
                if (authors.isEmpty()
                                && paper.getAuthorsRaw() != null
                                && !paper.getAuthorsRaw().isBlank()) {

                        authors = Arrays.stream(
                                        paper.getAuthorsRaw().split(","))
                                        .map(String::trim)
                                        .filter(name -> !name.isBlank())
                                        .distinct()
                                        .toList();
                }

                List<String> keywords = paper.getKeywords() == null
                                ? List.of()
                                : paper.getKeywords()
                                                .stream()
                                                .map(Keyword::getTerm)
                                                .filter(term -> term != null && !term.isBlank())
                                                .distinct()
                                                .toList();

                List<String> topics = paper.getResearchTopics() == null
                                ? List.of()
                                : paper.getResearchTopics()
                                                .stream()
                                                .map(ResearchTopic::getName)
                                                .filter(name -> name != null && !name.isBlank())
                                                .distinct()
                                                .toList();

                return new PaperResponse(
                                paper.getResearchPaperId(),
                                paper.getExternalId(),
                                paper.getTitle(),
                                paper.getAbstractText(),
                                paper.getYear(),
                                paper.getDoi(),
                                paper.getCitationCount(),
                                paper.getSourceApi(),
                                authors,
                                keywords,
                                paper.getJournal() != null
                                                ? paper.getJournal().getTitle()
                                                : null,
                                topics);
        }
}