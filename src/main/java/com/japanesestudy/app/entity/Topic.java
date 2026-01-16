package com.japanesestudy.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics", indexes = {
    @Index(name = "idx_topics_course_id", columnList = "course_id"),
    @Index(name = "idx_topics_course_order", columnList = "course_id, order_index")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"course", "studyItems"})
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Topic title is required")
    private String title;

    @Column(length = 1000)
    private String description;

    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<StudyItem> studyItems = new ArrayList<>();

    public Topic(String title, String description, Integer orderIndex) {
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
    }
}
