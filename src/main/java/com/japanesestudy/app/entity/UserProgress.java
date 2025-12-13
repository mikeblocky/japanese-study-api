package com.japanesestudy.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"user_id", "study_item_id"})},
        indexes = {
            @Index(name = "idx_user_progress_user_id", columnList = "user_id"),
            @Index(name = "idx_user_progress_next_review", columnList = "next_review"),
            @Index(name = "idx_user_progress_user_next_review", columnList = "user_id, next_review"),
            @Index(name = "idx_user_progress_study_item_id", columnList = "study_item_id")
        })
@Getter
@Setter
@NoArgsConstructor
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

    private LocalDateTime nextReview;
    private LocalDateTime lastReviewed;

    // SRS specific fields
    private Integer intervalDays = 0; // Current interval in days
    private Double easeFactor = 2.5; // SM-2 ease factor
    private Integer streak = 0; // Consecutive correct answers (or simplified version)

    public UserProgress(User user, StudyItem studyItem) {
        this.user = user;
        this.studyItem = studyItem;
        this.nextReview = LocalDateTime.now();
    }
}
