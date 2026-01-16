package com.japanesestudy.app.dto.importing;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class AnkiItem {
    private String front;
    private String back;
    private String deck;
    private String topic;
    private String reading;
    private Map<String, String> fields;
}
