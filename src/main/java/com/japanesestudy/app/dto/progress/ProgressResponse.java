package com.japanesestudy.app.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning progress data to the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private Long id;
    private Long studyItemId;
    private String primaryText;
    private String secondaryText;
    private Integer correctCount;
    private Integer incorrectCount;
    private Integer masteryLevel;
    private LocalDateTime lastStudied;
    private LocalDateTime nextReviewDate;
}
