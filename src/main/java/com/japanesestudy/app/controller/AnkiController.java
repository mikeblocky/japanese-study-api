package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.service.AnkiImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/anki")
@RequiredArgsConstructor
public class AnkiController {

    private final AnkiImportService ankiImportService;

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importAnki(@Valid @NonNull @RequestBody AnkiImportRequest request) {
        // GlobalExceptionHandler automatically handles any exceptions
        Map<String, Object> result = ankiImportService.importAnki(request);
        return ResponseEntity.ok(result);
    }
}
