package com.japanesestudy.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_logs")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"session", "studyItem"})
public class SessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_item_id", nullable = false)
    private StudyItem studyItem;

    private boolean correct;
    private LocalDateTime timestamp;

    public SessionLog(StudySession session, StudyItem studyItem, boolean correct) {
        this.timestamp = LocalDateTime.now();
        this.session = session;
        this.studyItem = studyItem;
        this.correct = correct;
    }
}
