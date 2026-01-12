package com.japanesestudy.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses", indexes = {
    @Index(name = "idx_courses_owner", columnList = "owner_id"),
    @Index(name = "idx_courses_visibility", columnList = "visibility")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"topics", "owner"})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Course title is required")
    private String title;

    @Column(length = 1000)
    private String description;

    // Level Range (replaces single "level" field)
    private String minLevel; // e.g., "N5", "Beginner"
    private String maxLevel; // e.g., "N3", "Advanced"
    
    // For backward compatibility - computed from minLevel/maxLevel
    @Deprecated
    private String level;

    // Advanced Settings
    private String tags; // Comma-separated: "kanji,vocabulary,JLPT"
    private String category; // e.g., "Vocabulary", "Grammar", "Reading", "Mixed"
    
    @Min(1) @Max(5)
    private Integer difficulty; // 1-5 scale
    
    private Integer estimatedHours; // Estimated study time

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Topic> topics = new ArrayList<>();

    public Course(String title, String description, String level) {
        this.title = title;
        this.description = description;
        this.level = level;
        this.minLevel = level;
        this.maxLevel = level;
    }

    // Expose owner username in JSON without exposing the full User object
    @JsonProperty("ownerUsername")
    public String getOwnerUsername() {
        return owner != null ? owner.getUsername() : null;
    }

    @JsonProperty("ownerId")
    public Long getOwnerId() {
        return owner != null ? owner.getId() : null;
    }

    // Computed level display (e.g., "N5 → N3" or just "N5")
    @JsonProperty("levelDisplay")
    public String getLevelDisplay() {
        if (minLevel == null && maxLevel == null) {
            return level != null ? level : "All Levels";
        }
        if (minLevel != null && maxLevel != null && !minLevel.equals(maxLevel)) {
            return minLevel + " → " + maxLevel;
        }
        return minLevel != null ? minLevel : maxLevel;
    }
}

