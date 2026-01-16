package com.japanesestudy.app.dto.importing;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AnkiImportRequest {
    private String courseName;
    private String description;
    private List<AnkiItem> items;
}
