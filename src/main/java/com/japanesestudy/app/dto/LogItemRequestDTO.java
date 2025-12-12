package com.japanesestudy.app.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for logging an item result.
 */
public class LogItemRequestDTO {

    private Long itemId;
    private boolean correct;

    public LogItemRequestDTO() {
    }

    @JsonCreator
    public LogItemRequestDTO(
            @JsonProperty("itemId") Long itemId,
            @JsonProperty("correct") boolean correct) {
        this.itemId = itemId;
        this.correct = correct;
    }

    // Record-style accessors
    public Long itemId() {
        return itemId;
    }

    public boolean correct() {
        return correct;
    }

    // Bean accessors
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
