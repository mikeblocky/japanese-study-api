package com.japanesestudy.app.entity;

import java.time.LocalDateTime;
import java.util.Objects;

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
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Integer intervalDays; // Current interval in days
    private Double easeFactor; // SM-2 ease factor
    private Integer streak; // Consecutive correct answers (or simplified version)

    // Constructors
    public UserProgress() {
        this.intervalDays = 0;
        this.easeFactor = 2.5;
        this.streak = 0;
    }

    public UserProgress(User user, StudyItem studyItem) {
        this();
        this.user = user;
        this.studyItem = studyItem;
        this.nextReview = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public StudyItem getStudyItem() {
        return studyItem;
    }

    public void setStudyItem(StudyItem studyItem) {
        this.studyItem = studyItem;
    }

    public LocalDateTime getNextReview() {
        return nextReview;
    }

    public void setNextReview(LocalDateTime nextReview) {
        this.nextReview = nextReview;
    }

    public LocalDateTime getLastReviewed() {
        return lastReviewed;
    }

    public void setLastReviewed(LocalDateTime lastReviewed) {
        this.lastReviewed = lastReviewed;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Double getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(Double easeFactor) {
        this.easeFactor = easeFactor;
    }

    public Integer getStreak() {
        return streak;
    }

    public void setStreak(Integer streak) {
        this.streak = streak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserProgress that = (UserProgress) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
