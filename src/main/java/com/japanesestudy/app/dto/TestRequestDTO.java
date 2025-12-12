package com.japanesestudy.app.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request DTO for generating a test.
 */
public class TestRequestDTO {

    private List<Long> topicIds;
    private int count;

    public TestRequestDTO() {
    }

    @JsonCreator
    public TestRequestDTO(
            @JsonProperty("topicIds") List<Long> topicIds,
            @JsonProperty("count") int count) {
        this.topicIds = topicIds;
        this.count = (count <= 0) ? 10 : count;
    }

    // Record-style accessors
    public List<Long> topicIds() {
        return topicIds;
    }

    public int count() {
        return count;
    }

    // Bean accessors
    public List<Long> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(List<Long> topicIds) {
        this.topicIds = topicIds;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = (count <= 0) ? 10 : count;
    }
}
