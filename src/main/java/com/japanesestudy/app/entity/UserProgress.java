package com.japanesestudy.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks per-user progress on study items.
 * Each user has independent progress that doesn't affect other users.
 */
@Entity
@Table(name = "user_progress", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "study_item_id"}),
       indexes = {
           @Index(name = "idx_progress_user", columnList = "user_id"),
           @Index(name = "idx_progress_user_item", columnList = "user_id, study_item_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "studyItem"})
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_item_id", nullable = false)
    private StudyItem studyItem;

    @Builder.Default
    private Integer correctCount = 0;

    @Builder.Default
    private Integer incorrectCount = 0;

    private LocalDateTime lastStudied;

    private LocalDateTime nextReviewDate;

    /**
     * Mastery level from 0-5 (based on spaced repetition principles)
     * 0 = New, 1-2 = Learning, 3-4 = Reviewing, 5 = Mastered
     */
    @Builder.Default
    private Integer masteryLevel = 0;

    /**
     * Records a study result and updates mastery.
     */
    public void recordResult(boolean correct) {
        if (correct) {
            correctCount++;
            if (masteryLevel < 5) {
                masteryLevel++;
            }
        } else {
            incorrectCount++;
            // Drop mastery but not below 0
            masteryLevel = Math.max(0, masteryLevel - 1);
        }
        lastStudied = LocalDateTime.now();
        calculateNextReview();
    }

    /**
     * Simple spaced repetition: next review based on mastery level.
     */
    private void calculateNextReview() {
        int daysUntilReview = switch (masteryLevel) {
            case 0 -> 0;  // Same day
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 14;
            case 5 -> 30;
            default -> 1;
        };
        nextReviewDate = LocalDateTime.now().plusDays(daysUntilReview);
    }
}
