package com.japanesestudy.app.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling media files (images, audio) from Anki imports.
 * Stores media to filesystem and provides URLs for retrieval.
 */
@Service
@Slf4j
public class MediaService {

    @Value("${app.media.storage-path:./media}")
    private String storagePath;

    @Value("${app.media.base-url:/api/media}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses the Anki media mapping file.
     * The media file in .apkg contains a JSON mapping of numeric filenames to original names.
     * Example: {"0": "audio.mp3", "1": "image.jpg"}
     */
    public Map<String, String> parseMediaMapping(File tempDir) {
        Map<String, String> mediaMap = new HashMap<>();
        
        File mediaFile = new File(tempDir, "media");
        if (!mediaFile.exists()) {
            log.debug("No media mapping file found in apkg");
            return mediaMap;
        }

        try {
            String content = Files.readString(mediaFile.toPath());
            if (content.trim().isEmpty() || content.trim().equals("{}")) {
                log.debug("Media mapping file is empty");
                return mediaMap;
            }
            
            mediaMap = objectMapper.readValue(content, new TypeReference<Map<String, String>>() {});
            log.info("Found {} media files in mapping", mediaMap.size());
            
        } catch (Exception e) {
            log.warn("Failed to parse media mapping: {}", e.getMessage());
        }

        return mediaMap;
    }

    /**
     * Extracts and stores media files from the temp directory.
     * Returns a map of original filename -> stored URL.
     */
    public Map<String, String> extractAndStoreMedia(File tempDir, Map<String, String> mediaMapping) {
        Map<String, String> storedUrls = new HashMap<>();
        
        if (mediaMapping.isEmpty()) {
            return storedUrls;
        }

        // Ensure storage directory exists
        Path storageDir = Paths.get(storagePath);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            log.error("Failed to create media storage directory: {}", e.getMessage());
            return storedUrls;
        }

        // Generate a unique prefix for this import to avoid collisions
        String importPrefix = UUID.randomUUID().toString().substring(0, 8);

        for (Map.Entry<String, String> entry : mediaMapping.entrySet()) {
            String numericName = entry.getKey();  // e.g., "0", "1", "2"
            String originalName = entry.getValue(); // e.g., "audio.mp3", "image.jpg"
            
            File sourceFile = new File(tempDir, numericName);
            if (!sourceFile.exists()) {
                log.debug("Media file {} not found in temp dir", numericName);
                continue;
            }

            try {
                // Generate stored filename with import prefix
                String storedName = importPrefix + "_" + sanitizeFilename(originalName);
                Path destPath = storageDir.resolve(storedName);
                
                Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Store the URL mapping
                String url = baseUrl + "/" + storedName;
                storedUrls.put(originalName, url);
                
                log.debug("Stored media: {} -> {}", originalName, url);
                
            } catch (IOException e) {
                log.warn("Failed to store media file {}: {}", originalName, e.getMessage());
            }
        }

        log.info("Stored {} media files", storedUrls.size());
        return storedUrls;
    }

    /**
     * Replaces media references in text with actual URLs.
     * Handles: [sound:filename.mp3], <img src="filename.jpg">
     */
    public String replaceMediaReferences(String text, Map<String, String> mediaUrls) {
        if (text == null || mediaUrls.isEmpty()) {
            return text;
        }

        String result = text;

        // Replace [sound:filename] with audio URL placeholder
        for (Map.Entry<String, String> entry : mediaUrls.entrySet()) {
            String filename = entry.getKey();
            String url = entry.getValue();
            
            // Replace sound references
            result = result.replace("[sound:" + filename + "]", "[audio:" + url + "]");
            
            // Replace img src references
            result = result.replaceAll(
                "<img[^>]*src=[\"']?" + escapeRegex(filename) + "[\"']?[^>]*>",
                "[image:" + url + "]"
            );
        }

        return result;
    }

    /**
     * Extracts media references from text.
     * Returns map of type -> list of URLs.
     */
    public Map<String, java.util.List<String>> extractMediaFromText(String text) {
        Map<String, java.util.List<String>> media = new HashMap<>();
        media.put("audio", new java.util.ArrayList<>());
        media.put("image", new java.util.ArrayList<>());

        if (text == null) return media;

        // Extract [audio:url]
        java.util.regex.Pattern audioPattern = java.util.regex.Pattern.compile("\\[audio:([^\\]]+)\\]");
        java.util.regex.Matcher audioMatcher = audioPattern.matcher(text);
        while (audioMatcher.find()) {
            media.get("audio").add(audioMatcher.group(1));
        }

        // Extract [image:url]
        java.util.regex.Pattern imagePattern = java.util.regex.Pattern.compile("\\[image:([^\\]]+)\\]");
        java.util.regex.Matcher imageMatcher = imagePattern.matcher(text);
        while (imageMatcher.find()) {
            media.get("image").add(imageMatcher.group(1));
        }

        return media;
    }

    /**
     * Gets a media file from storage.
     */
    public Path getMediaFile(String filename) {
        return Paths.get(storagePath).resolve(sanitizeFilename(filename));
    }

    /**
     * Checks if a filename is a supported media type.
     */
    public boolean isSupportedMedia(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") ||
               lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
               lower.endsWith(".gif") || lower.endsWith(".webp");
    }

    /**
     * Sanitizes filename to prevent path traversal attacks.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        // Remove path separators and other dangerous characters
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_");
    }

    /**
     * Escapes special regex characters in a string.
     */
    private String escapeRegex(String str) {
        return str.replaceAll("([\\\\\\[\\](){}.*+?^$|])", "\\\\$1");
    }
}
