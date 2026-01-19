package com.japanesestudy.app.entity;

public enum AccessLevel {
    VIEW,
    EDIT;

    public boolean allowsEdit() {
        return this == EDIT;
    }

    public boolean allowsView() {
        return this == VIEW || this == EDIT;
    }
}
