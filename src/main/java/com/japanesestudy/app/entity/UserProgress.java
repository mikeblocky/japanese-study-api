package com.japanesestudy.app.entity;

import java.time.LocalDateTime;

import com.japanesestudy.app.model.SrsRating;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "user_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "study_item_id"}),
        indexes = {
            @Index(name = "idx_progress_user", columnList = "user_id"),
            @Index(name = "idx_progress_next_review", columnList = "user_id, next_review_date")
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
    private Boolean studied = false;

    private LocalDateTime lastStudied;

    private LocalDateTime nextReviewDate;

    @Builder.Default
    @Column(name = "review_interval")
    private Integer interval = 1;

    @Builder.Default
    private Double easeFactor = 2.0;

    public void recordResult(boolean correct) {
        recordResult(correct ? SrsRating.GOOD : SrsRating.AGAIN, false);
    }

    public void recordResult(boolean correct, boolean isHarsh) {
        recordResult(correct ? SrsRating.GOOD : SrsRating.AGAIN, isHarsh);
    }

    public void recordResult(SrsRating rating, boolean isHarsh) {
        studied = true;
        lastStudied = LocalDateTime.now();

        final double minEase = 1.3;
        final double maxEase = 2.5;

        if (rating == null) {
            rating = SrsRating.GOOD;
        }

        switch (rating) {
            case AGAIN -> {
                interval = 1;
                easeFactor = isHarsh ? minEase : Math.max(minEase, easeFactor - 0.3);
            }
            case HARD -> {
                interval = Math.max(1, (int) Math.ceil(interval * 1.2));
                easeFactor = Math.max(minEase, easeFactor - 0.15);
            }
            case GOOD -> {
                interval = Math.max(1, (int) Math.ceil(interval * easeFactor));
                easeFactor = Math.min(maxEase, easeFactor + 0.05);
            }
            case EASY -> {
                interval = Math.max(1, (int) Math.ceil(interval * (easeFactor + 0.3)));
                easeFactor = Math.min(maxEase, easeFactor + 0.15);
            }
            default -> {
                interval = Math.max(1, interval);
                easeFactor = Math.max(minEase, easeFactor);
            }
        }

        nextReviewDate = LocalDateTime.now().plusDays(interval);
    }
}
