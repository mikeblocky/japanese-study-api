package com.japanesestudy.app.dto.importing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single Anki card item during import.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnkiItem {
    private String front;
    private String back;
    private String deck;
    private String topic;    // Parsed topic/lesson name
    private String reading;  // Japanese reading (furigana)
}
