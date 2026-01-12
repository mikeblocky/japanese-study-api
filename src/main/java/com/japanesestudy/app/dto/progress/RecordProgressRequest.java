package com.japanesestudy.app.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for recording a study result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordProgressRequest {
    private Long studyItemId;
    private boolean correct;
}
