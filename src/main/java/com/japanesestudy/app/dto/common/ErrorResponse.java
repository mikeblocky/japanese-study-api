package com.japanesestudy.app.dto.common;

public record ErrorResponse(int status, String message, String details) {
    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}
