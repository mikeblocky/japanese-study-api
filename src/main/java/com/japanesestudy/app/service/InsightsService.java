package com.japanesestudy.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.japanesestudy.app.dto.insights.InsightDtos.ActivityLog;
import com.japanesestudy.app.dto.insights.InsightDtos.CourseInsight;
import com.japanesestudy.app.dto.insights.InsightDtos.DailyStudyPoint;
import com.japanesestudy.app.dto.insights.InsightDtos.ManagementInsightsResponse;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.repository.AuditLogRepository;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.repository.UserProgressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final UserProgressRepository userProgressRepository;
    private final AuditLogRepository auditLogRepository;

    public ManagementInsightsResponse getManagementInsights(Long userId, int activityLimit) {
        long courses = courseRepository.count();
        long lessons = topicRepository.countByDeletedFalse();
        long words = studyItemRepository.countByDeletedFalse();

        List<UserProgress> progressList = (userId == null)
                ? List.of()
                : userProgressRepository.findByUserId(userId);

        LocalDateTime now = LocalDateTime.now();

        long studied = progressList.stream()
                .filter(p -> Boolean.TRUE.equals(p.getStudied()))
                .count();

        long dueWithin48h = (userId == null)
                ? 0
                : progressList.stream()
                        .filter(p -> Boolean.TRUE.equals(p.getStudied()))
                        .filter(p -> p.getNextReviewDate() != null)
                        .filter(p -> !p.getNextReviewDate().isAfter(now.plusHours(48)))
                        .count();

        List<DailyStudyPoint> last30Days = buildDailySeries(progressList);

        List<CourseInsight> courseInsights = courseRepository.findAll().stream()
                .map(course -> {
                    long lessonCount = topicRepository.countByCourseIdAndDeletedFalse(course.getId());
                    long wordCount = studyItemRepository.countActiveByCourseId(course.getId());
                    long studiedInCourse = (userId == null)
                            ? 0
                            : userProgressRepository.countStudiedByUserAndCourse(userId, course.getId());
                    double progressPercent = wordCount == 0 ? 0 : (double) studiedInCourse * 100.0 / wordCount;
                    return new CourseInsight(course.getId(), course.getTitle(), lessonCount, wordCount, progressPercent,
                            course.getUpdatedAt());
                })
                .sorted(Comparator.comparingLong(CourseInsight::words).reversed())
                .toList();

        double avgCourseProgress = courseInsights.isEmpty()
                ? 0
                : courseInsights.stream().mapToDouble(CourseInsight::progressPercent).average().orElse(0);

        int cappedLimit = Math.min(Math.max(activityLimit, 1), 200);
        List<ActivityLog> recentActivity = auditLogRepository
                .findAll(PageRequest.of(0, cappedLimit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(log -> new ActivityLog(
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getDetails(),
                log.getActorUserId(),
                log.getCreatedAt()))
                .toList();

        return ManagementInsightsResponse.builder()
                .totalCourses(courses)
                .totalLessons(lessons)
                .totalWords(words)
                .studiedWords(studied)
                .dueForReview(dueWithin48h)
                .avgCourseProgress(avgCourseProgress)
                .courseInsights(courseInsights)
                .last30Days(last30Days)
                .recentActivity(recentActivity)
                .build();
    }

    private List<DailyStudyPoint> buildDailySeries(List<UserProgress> progressList) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(29);

        Map<LocalDate, Long> countByDay = progressList.stream()
                .filter(p -> p.getLastStudied() != null)
                .filter(p -> !p.getLastStudied().toLocalDate().isBefore(cutoff))
                .collect(groupingBy(p -> p.getLastStudied().toLocalDate(), counting()));

        List<DailyStudyPoint> series = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            series.add(new DailyStudyPoint(date, countByDay.getOrDefault(date, 0L)));
        }
        return series;
    }
}
