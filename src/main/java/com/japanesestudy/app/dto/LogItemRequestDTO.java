package com.japanesestudy.app.dto;

/**
 * Request DTO for logging an item result.
 */
public record LogItemRequestDTO(
        Long itemId,
        boolean correct) {
}
