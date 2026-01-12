package com.japanesestudy.app.dto.importing;

import com.japanesestudy.app.entity.Visibility;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request for importing Anki deck items.
 */
@Getter
@Setter
public class AnkiImportRequest {
    private String courseName;
    private String description;
    private Visibility visibility; // PUBLIC or PRIVATE
    private List<AnkiItem> items;
}

