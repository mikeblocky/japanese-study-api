package com.japanesestudy.app.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    private Long id;
    private Long studyItemId;
    private String primaryText;
    private String secondaryText;
    private Boolean studied;
    private Integer interval;
    private Double easeFactor;
    private LocalDateTime lastStudied;
    private LocalDateTime nextReviewDate;
}
