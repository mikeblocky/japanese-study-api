package com.japanesestudy.app.dto;

import java.time.LocalDate;

/**
 * DTO for session summary information. Field names match frontend expectations.
 */
public class SessionSummaryDTO {

    private Long id;
    private LocalDate date;
    private long duration; // minutes - frontend expects "duration"
    private int score;

    public SessionSummaryDTO() {
    }

    public SessionSummaryDTO(Long id, LocalDate date, long duration, int score) {
        this.id = id;
        this.date = date;
        this.duration = duration;
        this.score = score;
    }

    // Record-style accessors
    public Long id() {
        return id;
    }

    public LocalDate date() {
        return date;
    }

    public long duration() {
        return duration;
    }

    public int score() {
        return score;
    }

    // Bean accessors
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
