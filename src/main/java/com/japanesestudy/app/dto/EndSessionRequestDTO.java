package com.japanesestudy.app.dto;

/**
 * Request DTO for ending a session.
 */
public record EndSessionRequestDTO(
        Long durationSeconds) {
}
