package com.japanesestudy.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "study_items", indexes = {
    @Index(name = "idx_study_items_topic_id", columnList = "topic_id")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"topic", "progressRecords"})
public class StudyItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "primaryText is required")
    private String primaryText;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "secondaryText is required")
    private String secondaryText;

    @Column(columnDefinition = "TEXT")
    private String meaning;

    @Convert(converter = com.japanesestudy.app.util.JsonMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> additionalData = new HashMap<>();

    /** URL/path to image media for this study item */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** URL/path to audio media for this study item */
    @Column(columnDefinition = "TEXT")
    private String audioUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @JsonIgnore
    private Topic topic;

    @OneToMany(mappedBy = "studyItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserProgress> progressRecords = new ArrayList<>();

    public StudyItem(String primaryText, String secondaryText) {
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
    }

    /** Transient field for user-specific SRS interval (not persisted) */
    @Transient
    private Integer userSrsInterval;
}
