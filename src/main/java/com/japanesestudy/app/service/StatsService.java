package com.japanesestudy.app.service;

import com.japanesestudy.app.dto.SessionSummaryDTO;
import com.japanesestudy.app.dto.StatsSummaryDTO;
import com.japanesestudy.app.model.StudySession;
import com.japanesestudy.app.model.User;
import com.japanesestudy.app.repository.StudySessionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating study statistics.
 */
@Service
public class StatsService {

    private final StudySessionRepository sessionRepository;

    public StatsService(StudySessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Get comprehensive stats summary for a user.
     */
    public StatsSummaryDTO getStatsSummary(User user) {
        List<StudySession> sessions = sessionRepository.findByUserId(user.getId());

        // Total duration in seconds (frontend expects seconds and divides by 60)
        long totalSeconds = sessions.stream()
                .filter(s -> s.getEndTime() != null)
                .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toSeconds())
                .sum();

        long currentStreak = calculateStreak(sessions);

        List<SessionSummaryDTO> recentActivity = sessions.stream()
                .sorted(Comparator.comparing(StudySession::getStartTime).reversed())
                .limit(5)
                .map(s -> new SessionSummaryDTO(
                        s.getId(),
                        s.getStartTime().toLocalDate(),
                        s.getEndTime() != null ? Duration.between(s.getStartTime(), s.getEndTime()).toMinutes() : 0,
                        0))
                .collect(Collectors.toList());

        return new StatsSummaryDTO(totalSeconds, sessions.size(), currentStreak, recentActivity);
    }

    /**
     * Calculate current study streak (consecutive days).
     */
    public long calculateStreak(List<StudySession> sessions) {
        if (sessions.isEmpty())
            return 0;

        List<LocalDate> activityDates = sessions.stream()
                .map(s -> s.getStartTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        if (activityDates.isEmpty())
            return 0;

        LocalDate today = LocalDate.now();
        LocalDate lastActivity = activityDates.get(0);

        // Streak is broken if no activity today or yesterday
        if (!lastActivity.equals(today) && !lastActivity.equals(today.minusDays(1))) {
            return 0;
        }

        long streak = 0;
        LocalDate checkDate = activityDates.contains(today) ? today : today.minusDays(1);

        for (LocalDate date : activityDates) {
            if (date.equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (date.isBefore(checkDate)) {
                break; // Gap found
            }
        }
        return streak;
    }
}
