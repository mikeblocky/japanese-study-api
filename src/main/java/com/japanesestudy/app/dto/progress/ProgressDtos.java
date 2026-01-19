package com.japanesestudy.app.dto.progress;

import java.time.LocalDateTime;

import com.japanesestudy.app.model.SrsRating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        private SrsRating rating;
        private Boolean correct;
        private boolean harshMode;
    }
}
