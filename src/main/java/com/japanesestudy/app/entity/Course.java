package com.japanesestudy.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"topics"})
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

    private String level;
    private String minLevel;
    private String maxLevel;
    private String category;
    private Integer difficulty;
    private Integer estimatedHours;
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Topic> topics = new ArrayList<>();

    /** Transient field to expose owner ID in JSON without loading the full User entity */
    @Transient
    public Long getOwnerId() {
        return owner != null ? owner.getId() : null;
    }

    public Course(String title, String description, String level) {
        this.title = title;
        this.description = description;
        this.level = level;
    }
}
