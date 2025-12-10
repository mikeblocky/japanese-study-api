package com.japanesestudy.app.dto;

import java.util.List;

/**
 * DTO for stats summary information.
 * Field names match frontend expectations.
 */
public record StatsSummaryDTO(
                long totalDuration, // seconds - frontend expects "totalDuration"
                long totalSessions,
                long currentStreak,
                List<SessionSummaryDTO> recentActivity) {
}
