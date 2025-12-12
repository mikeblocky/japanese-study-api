package com.japanesestudy.app.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for ending a session.
 */
public class EndSessionRequestDTO {

    private Long durationSeconds;

    public EndSessionRequestDTO() {
    }

    @JsonCreator
    public EndSessionRequestDTO(@JsonProperty("durationSeconds") Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    // Record-style accessor
    public Long durationSeconds() {
        return durationSeconds;
    }

    // Bean accessors
    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
