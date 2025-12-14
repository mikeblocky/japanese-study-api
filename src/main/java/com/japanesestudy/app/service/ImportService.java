package com.japanesestudy.app.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.luben.zstd.ZstdInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.japanesestudy.app.dto.AnkiItem;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling .apkg file extraction and parsing.
 * Extracts reusable logic from ImportController for cleaner separation of concerns.
 */
@Service
@Slf4j
public class ImportService {

    /**
     * Extracts an .apkg (zip) file to a temporary directory.
     * 
     * @param file the uploaded .apkg file
     * @return the temporary directory containing extracted files
     */
    public File extractApkgToTempDir(MultipartFile file) throws Exception {
        File tempDir = Files.createTempDirectory("anki-import").toFile();

        log.debug("Extracting .apkg file to {}", tempDir.getAbsolutePath());
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

        // Log extracted files
        File[] files = tempDir.listFiles();
        if (files != null) {
            log.debug("Files extracted:");
            for (File f : files) {
                log.debug("  - {} ({} bytes)", f.getName(), f.length());
            }
        }

        return tempDir;
    }

    /**
     * Finds the Anki collection database file in the extracted directory.
     * Prioritizes .anki2 (SQLite) over .anki21b (protobuf, not supported).
     * 
     * @param tempDir the directory containing extracted apkg contents
     * @return the collection database file, or null if not found
     */
    public File findCollectionDatabase(File tempDir) {
        // Priority order: .anki2 > .anki21 (SQLite formats)
        // .anki21b is Zstandard-compressed SQLite - we decompress it
        
        File collectionFile = new File(tempDir, "collection.anki2");
        if (collectionFile.exists()) {
            return collectionFile;
        }

        collectionFile = new File(tempDir, "collection.anki21");
        if (collectionFile.exists()) {
            return collectionFile;
        }

        // .anki21b is Zstd-compressed SQLite (Anki 2.1.50+)
        // Decompress it to collection.anki2 for processing
        File anki21bFile = new File(tempDir, "collection.anki21b");
        if (anki21bFile.exists()) {
            log.info("Found .anki21b format - decompressing Zstandard file");
            File decompressedFile = new File(tempDir, "collection.anki2");
            try {
                decompressZstd(anki21bFile, decompressedFile);
                log.info("Successfully decompressed .anki21b to .anki2 ({} bytes)", decompressedFile.length());
                return decompressedFile;
            } catch (Exception e) {
                log.error("Failed to decompress .anki21b file: {}", e.getMessage());
                // Fall through to return null
            }
        }

        // Some exports put the database in a subfolder
        File[] files = tempDir.listFiles();
        if (files != null) {
            // Prefer .anki2 files
            for (File f : files) {
                if (f.getName().endsWith(".anki2")) {
                    return f;
                }
            }
            // Then .anki21
            for (File f : files) {
                if (f.getName().endsWith(".anki21")) {
                    return f;
                }
            }
            // Then .anki21b (try to decompress)
            for (File f : files) {
                if (f.getName().endsWith(".anki21b")) {
                    File decompressedFile = new File(tempDir, f.getName().replace(".anki21b", ".anki2"));
                    try {
                        decompressZstd(f, decompressedFile);
                        return decompressedFile;
                    } catch (Exception e) {
                        log.error("Failed to decompress {}: {}", f.getName(), e.getMessage());
                    }
                }
            }
        }

        return null;
    }

    /**
     * Decompresses a Zstandard-compressed file.
     */
    private void decompressZstd(File input, File output) throws Exception {
        try (FileInputStream fis = new FileInputStream(input);
             ZstdInputStream zis = new ZstdInputStream(fis);
             FileOutputStream fos = new FileOutputStream(output)) {
            
            byte[] buffer = new byte[65536];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    /**
     * Parses notes from an Anki SQLite database and converts them to AnkiItems.
     * 
     * @param collectionFile the Anki collection database file
     * @param skipMedia      whether to skip cards that only contain media
     * @return parsing result containing items, skipped count, and warnings
     */
    public ParseResult parseAnkiDatabase(File collectionFile, boolean skipMedia) throws Exception {
        List<AnkiItem> items = new ArrayList<>();
        int skippedItems = 0;
        List<String> warnings = new ArrayList<>();

        // Load SQLite driver
        Class.forName("org.sqlite.JDBC");

        String url = "jdbc:sqlite:" + collectionFile.getAbsolutePath();
        log.debug("Connecting to SQLite database: {}", url);

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Log available tables for debugging
            logDatabaseTables(conn);

            // Log total notes count
            try (ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM notes")) {
                if (countRs.next()) {
                    log.info("Total notes in database: {}", countRs.getInt("cnt"));
                }
            }

            // Query notes
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

                    // Debug first few cards
                    if (cardCount <= 5 && fields != null) {
                        log.debug("Card {} fields: {}", cardCount,
                                fields.substring(0, Math.min(100, fields.length())));
                    }

                    if (fields == null || fields.trim().isEmpty()) {
                        skippedItems++;
                        continue;
                    }

                    // Skip Anki version warning placeholder
                    if (fields.contains("Please update to the latest Anki version")) {
                        log.debug("Skipping Anki version warning card");
                        skippedItems++;
                        continue;
                    }

                    // Split fields by Anki's delimiter (0x1f)
                    String[] parts = fields.split("\\x1f");

                    String expression = parts.length > 0 ? cleanText(parts[0], skipMedia) : "";
                    String reading = parts.length > 1 ? cleanText(parts[1], skipMedia) : "";
                    String meaning = parts.length > 2 ? cleanText(parts[2], skipMedia) : "";

                    // Skip empty cards
                    if (expression.trim().isEmpty() && meaning.trim().isEmpty()) {
                        skippedItems++;
                        continue;
                    }

                    // Skip media-only cards if requested
                    if (skipMedia) {
                        boolean frontHasMedia = containsMedia(parts.length > 0 ? parts[0] : "");
                        boolean backHasMedia = containsMedia(parts.length > 1 ? parts[1] : "");

                        if ((frontHasMedia || backHasMedia) && expression.trim().isEmpty() && meaning.trim().isEmpty()) {
                            skippedItems++;
                            continue;
                        }
                    }

                    AnkiItem item = new AnkiItem();
                    item.setFront(truncate(expression, 500));
                    item.setReading(reading.isEmpty() ? null : truncate(reading, 500));
                    item.setBack(truncate(meaning, 1000));

                    // Group into lessons (20 cards per lesson)
                    int lessonNum = (items.size() / 20) + 1;
                    item.setTopic(String.format("Lesson %02d", lessonNum));

                    items.add(item);

                    // Warn if text was truncated
                    if (expression.length() > 500 || meaning.length() > 1000) {
                        if (!warnings.contains("Some text was truncated to fit database limits")) {
                            warnings.add("Some text was truncated to fit database limits");
                        }
                    }
                }
            }
        }

        log.info("Parsed {} cards, skipped {}", items.size(), skippedItems);
        return new ParseResult(items, skippedItems, warnings);
    }

    /**
     * Cleans text by removing HTML tags, media references, and decoding HTML entities.
     */
    public String cleanText(String text, boolean removeMedia) {
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
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Checks if text contains media references.
     */
    public boolean containsMedia(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("[sound:")
                || text.contains("<img")
                || text.contains("[anki:play:")
                || text.matches(".*<audio.*>.*")
                || text.matches(".*<video.*>.*");
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    public void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
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

    private void logDatabaseTables(Connection conn) throws Exception {
        log.debug("=== Anki Database Debug ===");
        try (ResultSet tables = conn.getMetaData().getTables(null, null, "%", null)) {
            log.debug("Available tables:");
            while (tables.next()) {
                log.debug("  - {}", tables.getString("TABLE_NAME"));
            }
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /**
     * Result of parsing an Anki database.
     */
    public record ParseResult(List<AnkiItem> items, int skippedItems, List<String> warnings) {}
}
