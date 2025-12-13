package com.japanesestudy.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "study_items", indexes = {
    @Index(name = "idx_study_items_topic_id", columnList = "topic_id"),
    @Index(name = "idx_study_items_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"topic", "userProgress", "sessionLogs"})
public class StudyItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "primaryText is required")
    private String primaryText;

    @Column(nullable = false)
    @NotBlank(message = "secondaryText is required")
    private String secondaryText;

    private String meaning;
    private String imageUrl;
    private String audioUrl;

    private String type; // VOCABULARY, KANJI, GRAMMAR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @JsonIgnore
    private Topic topic;

    // Cascade delete user progress when study item is deleted
    @OneToMany(mappedBy = "studyItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserProgress> userProgress = new ArrayList<>();

    // Cascade delete session logs when study item is deleted
    @OneToMany(mappedBy = "studyItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SessionLog> sessionLogs = new ArrayList<>();

    public StudyItem(String primaryText, String secondaryText, String type) {
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.type = type;
    }
}
