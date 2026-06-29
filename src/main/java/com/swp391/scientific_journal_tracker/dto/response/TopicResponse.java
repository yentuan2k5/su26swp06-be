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
        return new TopicResponse(
                topic.getResearchTopicId(),
                topic.getName(),
                topic.getDescription(),
                topic.getResearchPapers() != null ? (long) topic.getResearchPapers().size() : 0L,
                topic.getFollowers() != null ? (long) topic.getFollowers().size() : 0L);
    }
}