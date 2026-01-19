package com.japanesestudy.app.dto.insights;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

public class InsightDtos {

    @Builder
    public record ManagementInsightsResponse(
            long totalCourses,
            long totalLessons,
            long totalWords,
            long studiedWords,
            long dueForReview,
            double avgCourseProgress,
            List<CourseInsight> courseInsights,
            List<DailyStudyPoint> last30Days,
            List<ActivityLog> recentActivity) {

    }

    public record CourseInsight(
            Long id,
            String title,
            long lessons,
            long words,
            double progressPercent,
            LocalDateTime updatedAt) {

    }

    public record DailyStudyPoint(LocalDate date, long studiedCount) {

    }

    public record ActivityLog(
            String entityType,
            Long entityId,
            String action,
            String details,
            Long actorUserId,
            LocalDateTime createdAt) {

    }
}
