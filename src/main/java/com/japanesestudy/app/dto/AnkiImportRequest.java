package com.japanesestudy.app.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class AnkiImportRequest {
    @NotBlank(message = "Course name is required")
    private String courseName;
    private String description;
    private List<AnkiItem> items;

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AnkiItem> getItems() {
        return items != null ? items : List.of();
    }

    public void setItems(List<AnkiItem> items) {
        this.items = items;
    }
}
