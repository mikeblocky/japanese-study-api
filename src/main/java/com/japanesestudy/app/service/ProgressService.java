package com.japanesestudy.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.dto.progress.ProgressDtos.ProgressResponse;
import com.japanesestudy.app.dto.progress.ProgressDtos.ProgressStatsResponse;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.model.SrsRating;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final StudyItemRepository studyItemRepository;

    @Transactional(readOnly = true)
    public List<ProgressResponse> getAllProgress(Long userId) {
        return progressRepository.findActiveByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getTopicProgress(Long userId, Long topicId) {
        return progressRepository.findActiveByUserIdAndTopicId(userId, topicId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressStatsResponse getStats(Long userId) {
        List<UserProgress> allProgress = progressRepository.findActiveByUserId(userId);
        long totalStudied = allProgress.stream().filter(p -> Boolean.TRUE.equals(p.getStudied())).count();
        long dueForReview = progressRepository.findDueForReview(userId, LocalDateTime.now()).size();

        return ProgressStatsResponse.builder()
                .totalItemsStudied(totalStudied)
                .itemsDueForReview(dueForReview)
                .build();
    }

    @Transactional
    public java.util.List<ProgressResponse> getChallengeItems(Long userId, int limit) {
        List<UserProgress> allStudied = progressRepository.findActiveByUserId(userId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getStudied()))
                .collect(java.util.stream.Collectors.toList());

        java.util.Collections.shuffle(allStudied);

        return allStudied.stream()
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProgressResponse recordProgress(Long userId, Long studyItemId, SrsRating rating, boolean harshMode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        StudyItem item = studyItemRepository.findById(studyItemId)
                .orElseThrow(() -> new IllegalArgumentException("Study item not found: " + studyItemId));
        if (item.isDeleted()) {
            throw new IllegalArgumentException("Study item not found: " + studyItemId);
        }

        UserProgress progress = progressRepository.findByUserIdAndStudyItemId(userId, studyItemId)
                .orElseGet(() -> UserProgress.builder().user(user).studyItem(item).build());

        progress.recordResult(rating, harshMode);
        progress = progressRepository.save(progress);
        return toResponse(progress);
    }

    private ProgressResponse toResponse(UserProgress progress) {
        StudyItem item = progress.getStudyItem();
        if (item == null) {
            return ProgressResponse.builder()
                    .id(progress.getId())
                    .studyItemId(null)
                    .primaryText("<deleted>")
                    .secondaryText("")
                    .meaning("")
                    .studied(progress.getStudied())
                    .interval(progress.getInterval())
                    .easeFactor(progress.getEaseFactor())
                    .lastStudied(progress.getLastStudied())
                    .nextReviewDate(progress.getNextReviewDate())
                    .build();
        }
        return ProgressResponse.builder()
                .id(progress.getId())
                .studyItemId(item.getId())
                .primaryText(item.getPrimaryText())
                .secondaryText(item.getSecondaryText())
                .meaning(item.getMeaning())
                .studied(progress.getStudied())
                .interval(progress.getInterval())
                .easeFactor(progress.getEaseFactor())
                .lastStudied(progress.getLastStudied())
                .nextReviewDate(progress.getNextReviewDate())
                .build();
    }
}
