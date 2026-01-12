package com.japanesestudy.app.service;

import com.japanesestudy.app.dto.progress.ProgressResponse;
import com.japanesestudy.app.dto.progress.ProgressStatsResponse;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user study progress.
 * All operations are scoped to the authenticated user.
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final StudyItemRepository studyItemRepository;

    /**
     * Get all progress for a user.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getAllProgress(Long userId) {
        return progressRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get progress for items in a specific topic.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getTopicProgress(Long userId, Long topicId) {
        return progressRepository.findByUserIdAndTopicId(userId, topicId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get aggregated statistics for a user.
     */
    @Transactional(readOnly = true)
    public ProgressStatsResponse getStats(Long userId) {
        long totalStudied = progressRepository.countByUserId(userId);
        long mastered = progressRepository.countMasteredByUserId(userId);
        long fullyMastered = progressRepository.countFullyMasteredByUserId(userId);
        long correct = progressRepository.sumCorrectByUserId(userId);
        long incorrect = progressRepository.sumIncorrectByUserId(userId);
        long dueForReview = progressRepository.findDueForReview(userId, LocalDateTime.now()).size();

        double accuracy = 0.0;
        if (correct + incorrect > 0) {
            accuracy = (double) correct / (correct + incorrect) * 100;
        }

        return ProgressStatsResponse.builder()
                .totalItemsStudied(totalStudied)
                .itemsMastered(mastered)
                .itemsFullyMastered(fullyMastered)
                .totalCorrect(correct)
                .totalIncorrect(incorrect)
                .accuracyPercent(Math.round(accuracy * 10) / 10.0)
                .itemsDueForReview(dueForReview)
                .build();
    }

    /**
     * Record a study result for a user.
     * Creates a new progress record if none exists, otherwise updates the existing one.
     */
    @Transactional
    public ProgressResponse recordProgress(Long userId, Long studyItemId, boolean correct) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        StudyItem item = studyItemRepository.findById(studyItemId)
                .orElseThrow(() -> new IllegalArgumentException("Study item not found: " + studyItemId));

        UserProgress progress = progressRepository.findByUserIdAndStudyItemId(userId, studyItemId)
                .orElseGet(() -> UserProgress.builder()
                        .user(user)
                        .studyItem(item)
                        .correctCount(0)
                        .incorrectCount(0)
                        .masteryLevel(0)
                        .build());

        progress.recordResult(correct);
        progress = progressRepository.save(progress);

        return toResponse(progress);
    }

    /**
     * Get items due for review for a user.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getDueForReview(Long userId) {
        return progressRepository.findDueForReview(userId, LocalDateTime.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProgressResponse toResponse(UserProgress progress) {
        return ProgressResponse.builder()
                .id(progress.getId())
                .studyItemId(progress.getStudyItem().getId())
                .primaryText(progress.getStudyItem().getPrimaryText())
                .secondaryText(progress.getStudyItem().getSecondaryText())
                .correctCount(progress.getCorrectCount())
                .incorrectCount(progress.getIncorrectCount())
                .masteryLevel(progress.getMasteryLevel())
                .lastStudied(progress.getLastStudied())
                .nextReviewDate(progress.getNextReviewDate())
                .build();
    }
}
