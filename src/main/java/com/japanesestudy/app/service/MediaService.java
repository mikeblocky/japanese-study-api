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
     * Falls back to scanning numbered files if JSON parsing fails.
     */
    public Map<String, String> parseMediaMapping(File tempDir) {
        Map<String, String> mediaMap = new HashMap<>();
        
        File mediaFile = new File(tempDir, "media");
        if (mediaFile.exists()) {
            String content = "";
            try {
                content = Files.readString(mediaFile.toPath());
                log.info("Reading media mapping file, length: {}", content.length());
                
                // Try standard parsing
                mediaMap = objectMapper.readValue(content, new TypeReference<Map<String, String>>() {});
                log.info("Found {} media files in mapping", mediaMap.size());
                return mediaMap;
            } catch (Exception e) {
                log.warn("Failed to parse media mapping JSON: {}", e.getMessage());
                log.debug("Media file content sample: {}", content.substring(0, Math.min(content.length(), 200)));
                
                // Fallback: Dirty regex parsing for {"0": "file.mp3"} pattern
                try {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(\\d+)\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                    while (m.find()) {
                        mediaMap.put(m.group(1), m.group(2));
                    }
                    if (!mediaMap.isEmpty()) {
                        log.info("Recovered {} mappings via regex", mediaMap.size());
                        return mediaMap;
                    }
                } catch (Exception ex) {
                    log.error("Regex recovery failed: {}", ex.getMessage());
                }
            }
        } else {
            log.warn("'media' file not found in temp directory: {}", tempDir.getAbsolutePath());
        }
        
        // Fallback: scan for numbered files and detect their types
        log.info("Using fallback media detection - scanning for numbered files");
        File[] files = tempDir.listFiles();
        if (files != null) {
            log.info("Scanning {} files in temp dir", files.length);
            for (File file : files) {
                String name = file.getName();
                // Check if filename is a number
                if (name.matches("\\d+") && file.isFile() && file.length() > 0) {
                    String extension = detectMediaType(file);
                    if (extension != null) {
                        String originalName = name + extension;
                        mediaMap.put(name, originalName);
                        log.debug("Detected media file: {} -> {}", name, originalName);
                    } else {
                        log.warn("Could not detect media type for file: {}", name);
                    }
                }
            }
        }
        
        if (!mediaMap.isEmpty()) {
            log.info("Fallback detection found {} media files", mediaMap.size());
        }
        
        return mediaMap;
    }

    /**
     * Builds media mapping by extracting filenames from card content and matching with numbered files.
     * This is a fallback when the media JSON is missing or corrupted.
     */
    public Map<String, String> buildMediaMappingFromCards(File tempDir, java.util.List<String> allCardTexts) {
        Map<String, String> mediaMap = new HashMap<>();
        
        // First, try the normal parsing
        mediaMap = parseMediaMapping(tempDir);
        
        // If we got mappings with real names (not just "0.mp3"), return them
        boolean hasRealNames = mediaMap.values().stream()
            .anyMatch(name -> !name.matches("\\d+\\.[a-z0-9]+"));
        if (hasRealNames) {
            return mediaMap;
        }
        
        // Extract all unique media filenames from card content
        java.util.Set<String> referencedAudio = new java.util.LinkedHashSet<>();
        java.util.Set<String> referencedImages = new java.util.LinkedHashSet<>();
        
        java.util.regex.Pattern soundPattern = java.util.regex.Pattern.compile("\\[sound:([^\\]]+)\\]");
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile("<img[^>]*src=[\"']?([^\"'>\\s]+)[\"']?");
        
        for (String text : allCardTexts) {
            if (text == null) continue;
            
            java.util.regex.Matcher soundMatcher = soundPattern.matcher(text);
            while (soundMatcher.find()) {
                referencedAudio.add(soundMatcher.group(1));
            }
            
            java.util.regex.Matcher imgMatcher = imgPattern.matcher(text);
            while (imgMatcher.find()) {
                referencedImages.add(imgMatcher.group(1));
            }
        }
        
        log.info("Found {} audio and {} image references in cards", referencedAudio.size(), referencedImages.size());
        
        // Get numbered files by type
        java.util.List<File> audioFiles = new java.util.ArrayList<>();
        java.util.List<File> imageFiles = new java.util.ArrayList<>();
        
        File[] files = tempDir.listFiles();
        if (files != null) {
            // Sort by number
            java.util.Arrays.sort(files, (a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a.getName()), Integer.parseInt(b.getName()));
                } catch (NumberFormatException e) {
                    return a.getName().compareTo(b.getName());
                }
            });
            
            for (File file : files) {
                if (file.getName().matches("\\d+") && file.isFile()) {
                    String type = detectMediaType(file);
                    if (type != null) {
                        if (type.equals(".mp3") || type.equals(".wav") || type.equals(".ogg")) {
                            audioFiles.add(file);
                        } else {
                            imageFiles.add(file);
                        }
                    }
                }
            }
        }
        
        // Try to match by count - if counts match, assign in order
        java.util.List<String> audioList = new java.util.ArrayList<>(referencedAudio);
        java.util.List<String> imageList = new java.util.ArrayList<>(referencedImages);
        
        if (audioFiles.size() == audioList.size()) {
            log.info("Audio count matches! Mapping {} audio files", audioFiles.size());
            for (int i = 0; i < audioFiles.size(); i++) {
                mediaMap.put(audioFiles.get(i).getName(), audioList.get(i));
            }
        } else if (!audioFiles.isEmpty() && !audioList.isEmpty()) {
            log.warn("Audio count mismatch: {} files vs {} references", audioFiles.size(), audioList.size());
        }
        
        if (imageFiles.size() == imageList.size()) {
            log.info("Image count matches! Mapping {} image files", imageFiles.size());
            for (int i = 0; i < imageFiles.size(); i++) {
                mediaMap.put(imageFiles.get(i).getName(), imageList.get(i));
            }
        } else if (!imageFiles.isEmpty() && !imageList.isEmpty()) {
            log.warn("Image count mismatch: {} files vs {} references", imageFiles.size(), imageList.size());
        }
        
        return mediaMap;
    }

    /**
     * Detects media type by reading file magic bytes.
     */
    private String detectMediaType(File file) {
        try {
            byte[] header = new byte[12];
            try (var fis = new java.io.FileInputStream(file)) {
                int read = fis.read(header);
                if (read < 4) return null;
            }
            
            // MP3: starts with ID3 or 0xFF 0xFB/F3/F2
            if ((header[0] == 'I' && header[1] == 'D' && header[2] == '3') ||
                (header[0] == (byte) 0xFF && (header[1] & 0xE0) == 0xE0)) {
                return ".mp3";
            }
            // WAV: RIFF....WAVE
            if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F') {
                return ".wav";
            }
            // OGG: OggS
            if (header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S') {
                return ".ogg";
            }
            // PNG: 0x89 PNG
            if (header[0] == (byte) 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
                return ".png";
            }
            // JPEG: 0xFF 0xD8 0xFF
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return ".jpg";
            }
            // GIF: GIF87a or GIF89a
            if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F') {
                return ".gif";
            }
            // WEBP: RIFF....WEBP
            if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return ".webp";
            }
            // SVG: <svg
            if (header[0] == '<' && header[1] == 's' && header[2] == 'v' && header[3] == 'g') {
                return ".svg";
            }
            
            // Try robust probe
            String type = Files.probeContentType(file.toPath());
            if (type != null) {
                if (type.contains("audio/mpeg")) return ".mp3";
                if (type.contains("image/jpeg")) return ".jpg";
                if (type.contains("image/png")) return ".png";
                if (type.contains("audio/wav")) return ".wav";
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Failed to detect media type for {}: {}", file.getName(), e.getMessage());
            return null;
        }
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
