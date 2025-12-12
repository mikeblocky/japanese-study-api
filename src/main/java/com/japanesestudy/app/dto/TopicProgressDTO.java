package com.japanesestudy.app.dto;

/**
 * DTO for topic progress information.
 */
public class TopicProgressDTO {

    private Long topicId;
    private int totalItems;
    private int masteredItems;
    private int percentage;

    public TopicProgressDTO() {
    }

    public TopicProgressDTO(Long topicId, int totalItems, int masteredItems, int percentage) {
        this.topicId = topicId;
        this.totalItems = totalItems;
        this.masteredItems = masteredItems;
        this.percentage = percentage;
    }

    public static TopicProgressDTO empty(Long topicId) {
        return new TopicProgressDTO(topicId, 0, 0, 0);
    }

    // Record-style accessors (keeps call sites compatible if any exist)
    public Long topicId() {
        return topicId;
    }

    public int totalItems() {
        return totalItems;
    }

    public int masteredItems() {
        return masteredItems;
    }

    public int percentage() {
        return percentage;
    }

    // Bean accessors (Jackson-friendly)
    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getMasteredItems() {
        return masteredItems;
    }

    public void setMasteredItems(int masteredItems) {
        this.masteredItems = masteredItems;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }
}
