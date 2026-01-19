package com.japanesestudy.app.util;

import java.lang.reflect.Field;

public class EntityUtils {

    public static <T> void copyNonNullFields(T source, T target) {
        if (source == null || target == null) return;
        
        Class<?> clazz = source.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                if (value != null) {
                    field.set(target, value);
                }
            } catch (IllegalAccessException ignored) {
                // Skip fields that can't be accessed
            }
        }
    }
}
