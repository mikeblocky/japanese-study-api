package com.japanesestudy.app.controller;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.japanesestudy.app.dto.importing.AnkiImportRequest;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.AnkiImportService;
import com.japanesestudy.app.service.ImportService;
import com.japanesestudy.app.service.ImportService.ParseResult;
import com.japanesestudy.app.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

    private final AnkiImportService ankiImportService;
    private final ImportService importService;
    private final MediaService mediaService;
    private final UserRepository userRepository;

    @GetMapping("/health")
    public ResponseEntity<?> checkHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "message", "Import service is available",
            "sqliteAvailable", isSqliteAvailable()));
    }

    @PostMapping("/anki")
    public ResponseEntity<?> importAnkiFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "skipMedia", defaultValue = "true") boolean skipMedia,
            @RequestParam(value = "textOnly", defaultValue = "true") boolean textOnly,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".apkg")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid file. Please upload a .apkg file."));
        }

        // Get owner from current user
        User owner = null;
        if (userDetails != null) {
            owner = userRepository.findById(userDetails.getId()).orElse(null);
        }

        File tempDir = null;
        try {
            tempDir = importService.extractApkgToTempDir(file);
            File collectionFile = importService.findCollectionDatabase(tempDir);
            if (collectionFile == null || !collectionFile.exists()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid Anki deck: collection database not found."));
            }

            log.info("Using database file: {} ({} bytes)", collectionFile.getName(), collectionFile.length());
            ParseResult parseResult = importService.parseAnkiDatabase(collectionFile, skipMedia);

            if (parseResult.items().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "No valid text cards found in the deck.",
                    "skippedItems", parseResult.skippedItems()));
            }

            AnkiImportRequest request = buildImportRequest(file, parseResult);

            // Extract and store media files if not skipping media
            Map<String, String> mediaUrls = Collections.emptyMap();
            if (!skipMedia) {
                Map<String, String> mediaMapping = mediaService.parseMediaMapping(tempDir);
                if (!mediaMapping.isEmpty()) {
                    log.info("Found {} media files to extract", mediaMapping.size());
                    mediaUrls = mediaService.extractAndStoreMedia(tempDir, mediaMapping);
                    log.info("Stored {} media files", mediaUrls.size());
                }
            }

            Map<String, Object> result = ankiImportService.importAnki(request, owner, mediaUrls);
            result.put("skippedItems", parseResult.skippedItems());
            result.put("warnings", parseResult.warnings());
            result.put("coursesCreated", 1);
            result.put("mediaFilesStored", mediaUrls.size());
            return ResponseEntity.ok(result);

        } catch (java.sql.SQLException e) {
            log.error("SQL exception while reading Anki deck", e);
            return buildErrorResponse("Database error while reading Anki deck.", e.getMessage());
        } catch (java.util.zip.ZipException e) {
            log.error("Zip exception while extracting .apkg file", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or corrupted .apkg file.", "error", "ZipException: " + e.getMessage()));
        } catch (java.io.IOException e) {
            log.error("IO exception during Anki import", e);
            return buildErrorResponse("File system error.", e.getMessage());
        } catch (ClassNotFoundException e) {
            log.error("SQLite driver not found", e);
            return buildErrorResponse("SQLite driver not available", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected exception during Anki import", e);
            return buildErrorResponse("Failed to import Anki deck: " + e.getMessage(), e.getClass().getSimpleName());
        } finally {
            importService.deleteDirectory(tempDir);
        }
    }

    private boolean isSqliteAvailable() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private AnkiImportRequest buildImportRequest(MultipartFile file, ParseResult parseResult) {
        AnkiImportRequest request = new AnkiImportRequest();
        String courseName = file.getOriginalFilename().replace(".apkg", "").trim();
        if (courseName.isEmpty()) courseName = "Imported Course";
        request.setCourseName(courseName);
        request.setDescription("Imported from Anki deck");
        request.setItems(parseResult.items());
        return request;
    }

    private ResponseEntity<?> buildErrorResponse(String message, String details) {
        return ResponseEntity.internalServerError()
            .body(new com.japanesestudy.app.dto.common.ErrorResponse(500, message, details));
    }
}
