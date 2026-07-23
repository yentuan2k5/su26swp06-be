package com.swp391.scientific_journal_tracker.dto.response;

import com.swp391.scientific_journal_tracker.entity.ResearchTopic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponse {

    private Long researchTopicId;
    private String name;
    private String description;
    private Long paperCount;
    private Long followerCount;

    public static TopicResponse fromEntity(ResearchTopic topic) {
        /*
         * Không gọi topic.getResearchPapers().size() hoặc
         * topic.getFollowers().size() ở đây.
         *
         * Hai collection này là lazy collection; khi database lớn,
         * .size() có thể khiến Hibernate tải rất nhiều entity vào RAM.
         * Những API cần số lượng sẽ dùng query COUNT riêng trong service.
         */
        return new TopicResponse(
                topic.getResearchTopicId(),
                topic.getName(),
                topic.getDescription(),
                0L,
                0L);
    }
}
