package com.japanesestudy.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AnkiImportRequest {

    @NotBlank(message = "Course name is required")
    private String courseName;
    private String description;
    private List<AnkiItem> items = new ArrayList<>();
}
