package com.japanesestudy.app.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.service.AnkiImportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/anki")
public class AnkiController {

    private final AnkiImportService ankiImportService;

    public AnkiController(AnkiImportService ankiImportService) {
        this.ankiImportService = ankiImportService;
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importAnki(@Valid @NonNull @RequestBody AnkiImportRequest request) {
        try {
            Map<String, Object> result = ankiImportService.importAnki(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Import failed: " + e.getMessage()));
        }
    }
}
