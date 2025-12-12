package com.japanesestudy.app.dto;

import java.util.List;

/**
 * DTO for stats summary information. Field names match frontend expectations.
 */
public class StatsSummaryDTO {

    private long totalDuration; // seconds - frontend expects "totalDuration"
    private long totalSessions;
    private long currentStreak;
    private List<SessionSummaryDTO> recentActivity;

    public StatsSummaryDTO() {
    }

    public StatsSummaryDTO(long totalDuration, long totalSessions, long currentStreak, List<SessionSummaryDTO> recentActivity) {
        this.totalDuration = totalDuration;
        this.totalSessions = totalSessions;
        this.currentStreak = currentStreak;
        this.recentActivity = recentActivity;
    }

    // Record-style accessors
    public long totalDuration() {
        return totalDuration;
    }

    public long totalSessions() {
        return totalSessions;
    }

    public long currentStreak() {
        return currentStreak;
    }

    public List<SessionSummaryDTO> recentActivity() {
        return recentActivity;
    }

    // Bean accessors
    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public long getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(long currentStreak) {
        this.currentStreak = currentStreak;
    }

    public List<SessionSummaryDTO> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<SessionSummaryDTO> recentActivity) {
        this.recentActivity = recentActivity;
    }
}
