package com.japanesestudy.app.dto;

import java.util.List;

/**
 * Request DTO for generating a test.
 */
public record TestRequestDTO(
        List<Long> topicIds,
        int count) {
    public TestRequestDTO {
        if (count <= 0)
            count = 10; // Default
    }
}
