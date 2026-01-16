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
    @Index(name = "idx_study_items_type", columnList = "item_type")
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

    @Column(nullable = false)
    @NotBlank(message = "primaryText is required")
    private String primaryText;

    @Column(nullable = false)
    @NotBlank(message = "secondaryText is required")
    private String secondaryText;

    private String meaning;
    private String imageUrl;
    private String audioUrl;
    @Column(name = "item_type")
    private String type;

    @Convert(converter = com.japanesestudy.app.util.JsonMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private java.util.Map<String, String> additionalData = new java.util.HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @JsonIgnore
    private Topic topic;

    @OneToMany(mappedBy = "studyItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserProgress> progressRecords = new ArrayList<>();

    public StudyItem(String primaryText, String secondaryText, String type) {
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.type = type;
    }
}
