package com.japanesestudy.app.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.japanesestudy.app.dto.importing.AnkiImportRequest;
import com.japanesestudy.app.service.AnkiImportService;
import com.japanesestudy.app.service.ImportService;
import com.japanesestudy.app.service.ImportService.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

    private final AnkiImportService ankiImportService;
    private final ImportService importService;

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
            @RequestParam(value = "textOnly", defaultValue = "true") boolean textOnly) {

        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".apkg")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid file. Please upload a .apkg file."));
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
            Map<String, Object> result = ankiImportService.importAnki(request);
            result.put("skippedItems", parseResult.skippedItems());
            result.put("warnings", parseResult.warnings());
            result.put("coursesCreated", 1);
            return ResponseEntity.ok(result);

        } catch (java.sql.SQLException e) {
            log.error("SQL exception while reading Anki deck", e);
            return buildErrorResponse("Database error while reading Anki deck.", "SQLiteException: " + e.getMessage(), null);
        } catch (java.util.zip.ZipException e) {
            log.error("Zip exception while extracting .apkg file", e);
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or corrupted .apkg file.", "error", "ZipException: " + e.getMessage()));
        } catch (java.io.IOException e) {
            log.error("IO exception during Anki import", e);
            return buildErrorResponse("File system error.", "IOException: " + e.getMessage(), null);
        } catch (ClassNotFoundException e) {
            log.error("SQLite driver not found", e);
            return buildErrorResponse("SQLite driver not available", "ClassNotFoundException", null);
        } catch (Exception e) {
            log.error("Unexpected exception during Anki import", e);
            return buildErrorResponse("Failed to import Anki deck: " + e.getMessage(), e.getClass().getSimpleName(), null);
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

    private ResponseEntity<?> buildErrorResponse(String message, String error, String details) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        errorResponse.put("error", error);
        if (details != null) errorResponse.put("details", details);
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}
