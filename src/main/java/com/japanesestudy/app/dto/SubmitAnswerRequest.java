package com.japanesestudy.app.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubmitAnswerRequest {

    @Min(1)
    private long itemId;
    private boolean correct;
}
