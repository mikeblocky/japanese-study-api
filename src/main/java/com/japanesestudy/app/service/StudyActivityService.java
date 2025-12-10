package com.japanesestudy.app.service;

import com.japanesestudy.app.model.*;
import com.japanesestudy.app.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class StudyActivityService {
    private final StudySessionRepository sessionRepository;
    private final SessionLogRepository logRepository;
    private final MasteryRepository masteryRepository;
    private final StudyItemRepository itemRepository;

    public StudyActivityService(StudySessionRepository sessionRepository,
            SessionLogRepository logRepository,
            MasteryRepository masteryRepository,
            StudyItemRepository itemRepository) {
        this.sessionRepository = sessionRepository;
        this.logRepository = logRepository;
        this.masteryRepository = masteryRepository;
        this.itemRepository = itemRepository;
    }

    public java.util.List<StudyItem> generateTest(java.util.List<Long> topicIds, int count) {
        java.util.List<StudyItem> allItems;
        if (topicIds == null || topicIds.isEmpty()) {
            allItems = itemRepository.findAll();
        } else {
            allItems = itemRepository.findAll().stream()
                    .filter(i -> i.getTopic() != null && topicIds.contains(i.getTopic().getId()))
                    .toList();
        }

        return allItems.stream()
                .sorted((a, b) -> Math.random() > 0.5 ? 1 : -1)
                .limit(count)
                .toList();
    }

    public StudySession startSession(User user) {
        StudySession session = new StudySession();
        session.setUser(user);
        session.setStartTime(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public StudySession endSession(Long sessionId, Long durationSeconds) {
        Objects.requireNonNull(sessionId);
        StudySession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setEndTime(LocalDateTime.now());
        if (durationSeconds != null) {
            session.setDurationSeconds(durationSeconds);
        }
        return sessionRepository.save(session);
    }

    public SessionLog logItem(Long sessionId, StudyItem item, boolean isCorrect) {
        Objects.requireNonNull(sessionId);
        StudySession session = sessionRepository.findById(sessionId).orElseThrow();

        SessionLog log = new SessionLog();
        log.setSession(session);
        log.setItem(item);
        log.setIsCorrect(isCorrect);
        logRepository.save(log);

        updateMastery(session.getUser(), item, isCorrect);
        return log;
    }

    private void updateMastery(User user, StudyItem item, boolean isCorrect) {
        Mastery mastery = masteryRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()) && m.getItem().getId().equals(item.getId()))
                .findFirst()
                .orElse(new Mastery());

        if (mastery.getId() == null) {
            mastery.setUser(user);
            mastery.setItem(item);
            mastery.setSrsLevel(0);
        }

        if (isCorrect) {
            mastery.setSrsLevel(mastery.getSrsLevel() + 1);
        } else {
            mastery.setSrsLevel(Math.max(0, mastery.getSrsLevel() - 1));
        }
        mastery.setLastReviewedAt(LocalDateTime.now());
        // Simple logic: review again in 1 day * level
        mastery.setNextReviewAt(LocalDateTime.now().plusDays(Math.max(1, mastery.getSrsLevel())));

        masteryRepository.save(mastery);
    }

    public java.util.List<StudyItem> getDueItems(User user) {
        return masteryRepository.findAllByUserIdAndNextReviewAtLessThanEqual(user.getId(), LocalDateTime.now())
                .stream()
                .map(Mastery::getItem)
                .toList();
    }

    public void resetMastery(User user, Long itemId) {
        Mastery mastery = masteryRepository.findByUserIdAndItemId(user.getId(), itemId).orElse(null);
        if (mastery != null) {
            mastery.setSrsLevel(0);
            mastery.setNextReviewAt(LocalDateTime.now());
            masteryRepository.save(mastery);
            mastery.setNextReviewAt(LocalDateTime.now());
            masteryRepository.save(mastery);
        }
    }

    public java.util.List<Mastery> getAllMastery(User user) {
        return masteryRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .toList();
    }

    public java.util.Map<String, Object> getStats(User user) {
        java.util.List<StudySession> sessions = sessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .toList();

        long totalDuration = sessions.stream()
                .mapToLong(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
                .sum();

        // Calculate History (Last 30 days)
        java.util.Map<java.time.LocalDate, Integer> historyMap = new java.util.HashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 0; i < 30; i++) {
            historyMap.put(today.minusDays(i), 0);
        }

        for (StudySession s : sessions) {
            java.time.LocalDate date = s.getStartTime().toLocalDate();
            if (!date.isBefore(today.minusDays(30))) {
                historyMap.merge(date, 1, Integer::sum);
            }
        }

        java.util.List<java.util.Map<String, Object>> history = historyMap.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> java.util.Map.<String, Object>of("date", e.getKey().toString(), "count", e.getValue()))
                .toList();

        // Calculate Current Streak
        int currentStreak = 0;
        java.time.LocalDate checkDate = today;

        boolean hasActivity = historyMap.getOrDefault(today, 0) > 0;
        if (!hasActivity) {
            checkDate = today.minusDays(1);
            hasActivity = historyMap.getOrDefault(checkDate, 0) > 0;
        }

        if (hasActivity) {
            while (historyMap.getOrDefault(checkDate, 0) > 0) {
                currentStreak++;
                checkDate = checkDate.minusDays(1);
                if (checkDate.isBefore(today.minusDays(30)))
                    break;
            }
        }

        return java.util.Map.of(
                "totalDuration", totalDuration,
                "totalSessions", sessions.size(),
                "history", history,
                "currentStreak", currentStreak);
    }
}
