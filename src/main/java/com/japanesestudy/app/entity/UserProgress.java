package com.japanesestudy.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
        recordResult(correct, false);
    }

    public void recordResult(boolean correct, boolean isHarsh) {
        studied = true;
        lastStudied = LocalDateTime.now();
        
        if (correct) {
            interval = (int) Math.ceil(interval * easeFactor);
            easeFactor = Math.min(2.5, easeFactor + 0.1);
        } else {
            interval = 1;
            // Harsh mode: Reset ease factor to minimum immediately
            // Normal mode: Decrease gradually
            if (isHarsh) {
                easeFactor = 1.3;
            } else {
                easeFactor = Math.max(1.3, easeFactor - 0.2);
            }
        }
        
        nextReviewDate = LocalDateTime.now().plusDays(interval);
    }
}
