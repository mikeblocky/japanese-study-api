package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.common.ErrorResponse;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.AnkiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static com.japanesestudy.app.util.Utils.*;

@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final AnkiService ankiService;
    private final UserRepository userRepository;

    @PostMapping("/anki")
    public ResponseEntity<?> importAnki(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, "File is required"));
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".apkg")) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, "Only .apkg files are supported"));
        }
        try {
            var owner = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String deckName = filename != null ? filename : originalFilename;
            Map<String, Object> result = ankiService.importAnkiFile(file, deckName, owner);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Validation error during import", e);
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to import Anki deck", e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(500, "Import failed: " + e.getMessage(), e.toString()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getImportStatus() {
        return ok(Map.of("status", "ready", "supportedFormats", ".apkg"));
    }
}
