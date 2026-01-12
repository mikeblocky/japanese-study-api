package com.japanesestudy.app.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user progress statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressStatsResponse {
    private long totalItemsStudied;
    private long itemsMastered;        // masteryLevel >= 3
    private long itemsFullyMastered;   // masteryLevel = 5
    private long totalCorrect;
    private long totalIncorrect;
    private double accuracyPercent;
    private long itemsDueForReview;
}
