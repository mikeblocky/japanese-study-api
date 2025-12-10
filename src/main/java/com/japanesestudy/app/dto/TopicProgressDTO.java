package com.japanesestudy.app.dto;

/**
 * DTO for topic progress information.
 */
public record TopicProgressDTO(
        Long topicId,
        int totalItems,
        int masteredItems,
        int percentage) {
    public static TopicProgressDTO empty(Long topicId) {
        return new TopicProgressDTO(topicId, 0, 0, 0);
    }
}
