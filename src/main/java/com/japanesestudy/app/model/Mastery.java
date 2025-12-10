package com.japanesestudy.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Mastery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private StudyItem item;

    private Integer srsLevel; // 0 to 5, etc.
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt;

    public Mastery() {
    }

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

    public StudyItem getItem() {
        return item;
    }

    public void setItem(StudyItem item) {
        this.item = item;
    }

    public Integer getSrsLevel() {
        return srsLevel;
    }

    public void setSrsLevel(Integer srsLevel) {
        this.srsLevel = srsLevel;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }
}
