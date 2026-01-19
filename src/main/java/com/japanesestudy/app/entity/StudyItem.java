package com.japanesestudy.app.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "study_items", indexes = {
    @Index(name = "idx_study_items_topic_id", columnList = "topic_id")
})
@SQLDelete(sql = "UPDATE study_items SET deleted = true, version = version + 1 WHERE id = ? AND version = ?")
@Where(clause = "deleted = false")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @JsonIgnore
    private Topic topic;

    @OneToMany(mappedBy = "studyItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserProgress> progressRecords = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long version;

    public StudyItem(String primaryText, String secondaryText) {
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
    }

    /**
     * Transient field for user-specific SRS interval (not persisted)
     */
    @Transient
    private Integer userSrsInterval;
}
