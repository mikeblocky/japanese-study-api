package com.japanesestudy.app.service;

import com.japanesestudy.app.entity.SessionLog;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.StudySession;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.repository.SessionLogRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.StudySessionRepository;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final UserProgressRepository userProgressRepository;
    private final StudyItemRepository studyItemRepository;
    private final StudySessionRepository sessionRepository;
    private final SessionLogRepository sessionLogRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<UserProgress> getDueItems(long userId) {
        return userProgressRepository.findDueItems(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<StudyItem> getAllItems() {
        return studyItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<StudyItem> getItemsByTopic(long topicId) {
        return studyItemRepository.findByTopicId(topicId);
    }

    @Transactional
    public StudySession startSession(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        StudySession session = new StudySession(user);
        return sessionRepository.save(session);
    }

    @Transactional
    public void submitAnswer(long sessionId, long itemId, boolean correct) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        StudyItem item = studyItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item not found"));

        // Log the answer
        SessionLog log = new SessionLog(session, item, correct);
        sessionLogRepository.save(log);

        // Update Progress (SRS)
        updateProgress(session.getUser(), item, correct);
    }

    private void updateProgress(User user, StudyItem item, boolean correct) {
        long userId = Objects.requireNonNull(user.getId(), "user.id must not be null");
        long studyItemId = Objects.requireNonNull(item.getId(), "item.id must not be null");
        Optional<UserProgress> progressOpt = userProgressRepository.findByUserIdAndStudyItemId(userId, studyItemId);
        UserProgress progress = progressOpt.orElse(new UserProgress(user, item));

        if (correct) {
            int oldStreak = progress.getStreak();
            progress.setStreak(oldStreak + 1);

            // Simple logic: interval = 1, 3, 7, 14, 30... (approx)
            // Or just multiply existing interval
            int interval = progress.getIntervalDays();
            switch (interval) {
                case 0:
                    interval = 1;
                    break;
                case 1:
                    interval = 3;
                    break;
                default:
                    interval = (int) (interval * progress.getEaseFactor());
                    break;
            }

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
    public StudySession endSession(long sessionId) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        session.setEndTime(LocalDateTime.now());
        long duration = java.time.Duration.between(session.getStartTime(), session.getEndTime()).getSeconds();
        session.setDurationSeconds(duration);

        return sessionRepository.save(session);
    }
}
