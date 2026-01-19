package com.japanesestudy.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Discrete review ratings similar to Anki's 4-button flow.
 */
public enum SrsRating {
    AGAIN,
    HARD,
    GOOD,
    EASY;

    @JsonCreator
    public static SrsRating fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return SrsRating.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
