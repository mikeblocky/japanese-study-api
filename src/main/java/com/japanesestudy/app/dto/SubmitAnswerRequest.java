package com.japanesestudy.app.dto;

import jakarta.validation.constraints.Min;

public class SubmitAnswerRequest {

    @Min(1)
    private long itemId;
    private boolean correct;

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
