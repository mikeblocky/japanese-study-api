package com.japanesestudy.app.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressStatsResponse {
    private long totalItemsStudied;
    private long itemsDueForReview;
}
