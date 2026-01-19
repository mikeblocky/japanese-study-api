package com.japanesestudy.app.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.function.Supplier;

public class ControllerUtils {

    public static void validateOwnership(Long ownerId, Long userId) {
        if (ownerId != null && !ownerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public static <T> T getOrThrow(Supplier<java.util.Optional<T>> supplier, String message) {
        return supplier.get().orElseThrow(() -> new IllegalArgumentException(message));
    }

    public static <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    public static ResponseEntity<Map<String, String>> message(String msg) {
        return ResponseEntity.ok(Map.of("message", msg));
    }

    public static ResponseEntity<Map<String, String>> error(String msg) {
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    public static ResponseEntity<Map<String, String>> unauthorized(String msg) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", msg));
    }
}
