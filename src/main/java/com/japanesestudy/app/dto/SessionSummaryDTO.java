package com.japanesestudy.app.dto;

import java.time.LocalDate;

/**
 * DTO for session summary information.
 * Field names match frontend expectations.
 */
public record SessionSummaryDTO(
                Long id,
                LocalDate date,
                long duration, // minutes - frontend expects "duration"
                int score) {
}
