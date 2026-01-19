package com.japanesestudy.app.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

public class ProgressDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressResponse {
        private Long id;
        private Long studyItemId;
        private String primaryText;
        private String secondaryText;
        private String meaning;
        private Boolean studied;
        private Integer interval;
        private Double easeFactor;
        private LocalDateTime lastStudied;
        private LocalDateTime nextReviewDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressStatsResponse {
        private long totalItemsStudied;
        private long itemsDueForReview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordProgressRequest {
        private Long studyItemId;
        private boolean correct;
        private boolean harshMode;
    }
}
