package com.japanesestudy.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AnkiItem {
    private String front;
    private String reading;
    private String back;
    private String topic;
}
