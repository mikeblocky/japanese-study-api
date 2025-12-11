package com.japanesestudy.app.dto;

public class SubmitAnswerRequest {
    private Long itemId;
    private boolean correct;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
