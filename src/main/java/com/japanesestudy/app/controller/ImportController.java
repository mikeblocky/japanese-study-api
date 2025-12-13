package com.japanesestudy.app.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.dto.AnkiItem;
import com.japanesestudy.app.service.AnkiImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

    private final AnkiImportService ankiImportService;

    @GetMapping("/health")
    public ResponseEntity<?> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "message", "Import service is available",
                "sqliteAvailable", checkSQLiteAvailable()
        ));
    }

    private boolean checkSQLiteAvailable() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @PostMapping("/anki")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importAnkiFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "skipMedia", defaultValue = "true") boolean skipMedia,
            @RequestParam(value = "textOnly", defaultValue = "true") boolean textOnly) {

        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".apkg")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid file. Please upload a .apkg file."));
        }

        File tempDir = null;
        File collectionFile = null;

        try {
            // Create temporary directory
            tempDir = Files.createTempDirectory("anki-import").toFile();

            // Extract .apkg (which is a zip file) - extract ALL files to see what's inside
            log.debug("Extracting .apkg file");
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    log.debug("Found entry: {}", entry.getName());
                    File extractedFile = new File(tempDir, entry.getName());

                    if (!entry.isDirectory()) {
                        extractedFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(extractedFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                }
            }

            // List all files in temp directory
            log.debug("Files extracted to temp dir:");
            for (File f : tempDir.listFiles()) {
                log.debug("  - {} ({} bytes)", f.getName(), f.length());
            }

            // Find collection database - PRIORITIZE collection.anki21 (newer format with actual data)
            // collection.anki2 in newer exports is just a placeholder with the warning message
            collectionFile = new File(tempDir, "collection.anki21");
            if (!collectionFile.exists()) {
                collectionFile = new File(tempDir, "collection.anki21b");
            }
            if (!collectionFile.exists()) {
                collectionFile = new File(tempDir, "collection.anki2");
            }
            // Some exports put it in a subfolder
            if (!collectionFile.exists()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".anki21") || f.getName().endsWith(".anki21b") || f.getName().endsWith(".anki2")) {
                            collectionFile = f;
                            break;
                        }
                    }
                }
            }

            log.info("Using database file: {} ({} bytes)", collectionFile.getName(), collectionFile.length());

            if (!collectionFile.exists()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid Anki deck: collection database not found."));
            }

            // Read cards from SQLite database
            List<AnkiItem> items = new ArrayList<>();
            int skippedItems = 0;
            List<String> warnings = new ArrayList<>();

            // Explicitly load SQLite driver
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "message", "SQLite driver not available",
                        "error", "SQLite JDBC driver is not loaded. Please contact support."
                ));
            }

            // Verify file exists and is readable
            if (!collectionFile.exists() || !collectionFile.canRead()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Collection database file not found or not readable",
                        "error", "The .apkg file might be corrupted"
                ));
            }

            String url = "jdbc:sqlite:" + collectionFile.getAbsolutePath();
            log.debug("Attempting to connect to SQLite database: {}", url);

            try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {

                // Debug: Print available tables
                log.debug("=== Anki Database Debug ===");
                try (ResultSet tables = conn.getMetaData().getTables(null, null, "%", null)) {
                    log.debug("Available tables:");
                    while (tables.next()) {
                        log.debug("  - {}", tables.getString("TABLE_NAME"));
                    }
                }

                // Check how many notes exist
                try (ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM notes")) {
                    if (countRs.next()) {
                        log.info("Total notes in database: {}", countRs.getInt("cnt"));
                    }
                }

                // Query to get all notes directly (more reliable than joining cards)
                String query = """
                    SELECT 
                        id,
                        flds as fields,
                        sfld as sort_field
                    FROM notes
                    LIMIT 10000
                    """;

                try (ResultSet rs = stmt.executeQuery(query)) {
                    int cardCount = 0;
                    while (rs.next()) {
                        cardCount++;
                        String fields = rs.getString("fields");
                        String sortField = rs.getString("sort_field");

                        // Debug first few cards
                        if (cardCount <= 5) {
                            log.debug("Card {} fields: {}", cardCount, 
                                    fields != null ? fields.substring(0, Math.min(100, fields.length())) : "null");
                        }

                        if (fields == null || fields.trim().isEmpty()) {
                            skippedItems++;
                            continue;
                        }

                        // Skip the Anki version warning placeholder
                        if (fields.contains("Please update to the latest Anki version")) {
                            log.debug("Skipping Anki version warning card");
                            skippedItems++;
                            continue;
                        }

                        // Split fields by Anki's delimiter
                        String[] parts = fields.split("\\x1f");

                        // For Kanji decks: parts[0]=Expression, parts[1]=Reading, parts[2]=Meaning
                        String expression = parts.length > 0 ? cleanText(parts[0], skipMedia) : "";
                        String reading = parts.length > 1 ? cleanText(parts[1], skipMedia) : "";
                        String meaning = parts.length > 2 ? cleanText(parts[2], skipMedia) : "";

                        // Skip empty cards BEFORE checking for media
                        if (expression.trim().isEmpty() && meaning.trim().isEmpty()) {
                            skippedItems++;
                            continue;
                        }

                        // If skipMedia is true, check AFTER cleaning and skip only if no text remains
                        if (skipMedia) {
                            boolean frontHasMedia = containsMedia(parts.length > 0 ? parts[0] : "");
                            boolean backHasMedia = containsMedia(parts.length > 1 ? parts[1] : "");

                            // Only skip if card has media AND no meaningful text content
                            if ((frontHasMedia || backHasMedia) && expression.trim().isEmpty() && meaning.trim().isEmpty()) {
                                skippedItems++;
                                continue;
                            }
                        }

                        AnkiItem item = new AnkiItem();
                        // Map correctly: front=expression, reading=reading, back=meaning
                        item.setFront(expression.substring(0, Math.min(expression.length(), 500)));
                        item.setReading(reading.isEmpty() ? null : reading.substring(0, Math.min(reading.length(), 500)));
                        item.setBack(meaning.substring(0, Math.min(meaning.length(), 1000)));
                        // Use items.size() (0-based) for lesson grouping so cards 0-19 = Lesson 01, 20-39 = Lesson 02, etc.
                        int lessonNum = (items.size() / 20) + 1;
                        item.setTopic(String.format("Lesson %02d", lessonNum)); // Zero-padded for proper sorting
                        items.add(item);

                        // Warn if text was truncated
                        if (expression.length() > 500 || meaning.length() > 1000) {
                            warnings.add("Some text was truncated to fit database limits");
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No valid text cards found in the deck. All cards may contain media or be empty.",
                        "skippedItems", skippedItems
                ));
            }

            log.info("Successfully processed {} cards, skipped {}", items.size(), skippedItems);

            // Import using existing service
            AnkiImportRequest request = new AnkiImportRequest();
            String courseName = file.getOriginalFilename().replace(".apkg", "").trim();
            if (courseName.isEmpty()) {
                courseName = "Imported Course";
            }
            request.setCourseName(courseName);
            request.setDescription("Imported from Anki deck");
            request.setItems(items);

            Map<String, Object> result = ankiImportService.importAnki(request);
            result.put("skippedItems", skippedItems);
            result.put("warnings", warnings);
            result.put("coursesCreated", 1);

            return ResponseEntity.ok(result);

        } catch (java.sql.SQLException e) {
            log.error("SQL exception while reading Anki deck", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Database error while reading Anki deck. The file might be corrupted or not a valid Anki deck.");
            errorResponse.put("error", "SQLiteException: " + e.getMessage());
            errorResponse.put("details", "Make sure you exported a valid .apkg file from Anki Desktop (File > Export > Anki Deck Package)");
            return ResponseEntity.internalServerError().body(errorResponse);
        } catch (java.util.zip.ZipException e) {
            log.error("Zip exception while extracting .apkg file", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid or corrupted .apkg file. The file could not be extracted.");
            errorResponse.put("error", "ZipException: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (java.io.IOException e) {
            log.error("IO exception during Anki import", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "File system error. Unable to create temporary files. This might be a server configuration issue.");
            errorResponse.put("error", "IOException: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected exception during Anki import", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to import Anki deck: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(errorResponse);
        } finally {
            // Cleanup temp files
            if (tempDir != null && tempDir.exists()) {
                deleteDirectory(tempDir);
            }
        }
    }

    private String cleanText(String text, boolean removeMedia) {
        if (text == null) {
            return "";
        }

        // Remove HTML tags
        text = text.replaceAll("<[^>]+>", "");

        // Remove media references if requested
        if (removeMedia) {
            text = text.replaceAll("\\[sound:[^\\]]+\\]", "");
            text = text.replaceAll("<img[^>]+>", "");
            text = text.replaceAll("\\[anki:play:[^\\]]+\\]", "");
        }

        // Decode HTML entities
        text = text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"");

        // Clean up whitespace
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    private boolean containsMedia(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Only check for actual media tags, not file extensions that might be in text
        return text.contains("[sound:")
                || text.contains("<img")
                || text.contains("[anki:play:")
                || text.matches(".*<audio.*>.*")
                || text.matches(".*<video.*>.*");
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
