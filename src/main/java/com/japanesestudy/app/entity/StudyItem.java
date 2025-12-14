package com.japanesestudy.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Study item entity - vocabulary, kanji, or grammar item.
 */
@Entity
@Table(name = "study_items", indexes = {
    @Index(name = "idx_study_items_topic_id", columnList = "topic_id"),
    @Index(name = "idx_study_items_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "topic")
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

    public StudyItem(String primaryText, String secondaryText, String type) {
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.type = type;
    }
}
