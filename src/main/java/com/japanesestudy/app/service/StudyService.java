package com.japanesestudy.app.service;

import com.japanesestudy.app.entity.*;
import com.japanesestudy.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StudyService {

    @Autowired
    private UserProgressRepository userProgressRepository;

    @Autowired
    private StudyItemRepository studyItemRepository;

    @Autowired
    private StudySessionRepository sessionRepository;

    @Autowired
    private SessionLogRepository sessionLogRepository;

    @Autowired
    private UserRepository userRepository;

    public List<UserProgress> getDueItems(Long userId) {
        return userProgressRepository.findDueItems(userId, LocalDateTime.now());
    }

    public List<StudyItem> getItemsByTopic(Long topicId) {
        return studyItemRepository.findByTopicId(topicId);
    }

    @Transactional
    public StudySession startSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudySession session = new StudySession(user);
        return sessionRepository.save(session);
    }

    @Transactional
    public void submitAnswer(Long sessionId, Long itemId, boolean correct) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        StudyItem item = studyItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Log the answer
        SessionLog log = new SessionLog(session, item, correct);
        sessionLogRepository.save(log);

        // Update Progress (SRS)
        updateProgress(session.getUser(), item, correct);
    }

    private void updateProgress(User user, StudyItem item, boolean correct) {
        Optional<UserProgress> progressOpt = userProgressRepository.findByUserIdAndStudyItemId(user.getId(),
                item.getId());
        UserProgress progress = progressOpt.orElse(new UserProgress(user, item));

        if (correct) {
            int oldStreak = progress.getStreak();
            progress.setStreak(oldStreak + 1);

            // Simple logic: interval = 1, 3, 7, 14, 30... (approx)
            // Or just multiply existing interval
            int interval = progress.getIntervalDays();
            if (interval == 0)
                interval = 1;
            else if (interval == 1)
                interval = 3;
            else
                interval = (int) (interval * progress.getEaseFactor());

            progress.setIntervalDays(interval);
            progress.setNextReview(LocalDateTime.now().plusDays(interval));
        } else {
            progress.setStreak(0);
            progress.setIntervalDays(1); // Reset to 1 day
            progress.setNextReview(LocalDateTime.now().plusDays(1));
            // Decrease ease factor slightly?
            progress.setEaseFactor(Math.max(1.3, progress.getEaseFactor() - 0.2));
        }

        progress.setLastReviewed(LocalDateTime.now());
        userProgressRepository.save(progress);
    }

    @Transactional
    public StudySession endSession(Long sessionId) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setEndTime(LocalDateTime.now());
        long duration = java.time.Duration.between(session.getStartTime(), session.getEndTime()).getSeconds();
        session.setDurationSeconds(duration);

        return sessionRepository.save(session);
    }
}
