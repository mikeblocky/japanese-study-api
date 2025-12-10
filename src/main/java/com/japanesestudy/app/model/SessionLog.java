package com.japanesestudy.app.model;

import jakarta.persistence.*;

@Entity
public class SessionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private StudySession session;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private StudyItem item;

    private Boolean isCorrect; // Did the user get it right?
    private Integer timeTakenMs;

    public SessionLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudySession getSession() {
        return session;
    }

    public void setSession(StudySession session) {
        this.session = session;
    }

    public StudyItem getItem() {
        return item;
    }

    public void setItem(StudyItem item) {
        this.item = item;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Integer getTimeTakenMs() {
        return timeTakenMs;
    }

    public void setTimeTakenMs(Integer timeTakenMs) {
        this.timeTakenMs = timeTakenMs;
    }
}
