package com.japanesestudy.app.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.ZstdInputStream;
import com.japanesestudy.app.dto.importing.AnkiItem;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.util.Utils.EvictAllCaches;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnkiService {

    private static final int ZIP_BUFFER_SIZE = 8192;
    private static final int ZSTD_BUFFER_SIZE = 65536;
    private static final int MAX_EXPRESSION_LENGTH = 500;
    private static final int MAX_MEANING_LENGTH = 1000;
    private static final int MAX_READING_LENGTH = 500;
    private static final String ANKI_FIELD_DELIMITER_REGEX = "\\x1f";
    private static final String WARN_TRUNCATION = "Some text was truncated to fit database limits";
    private static final int ITEMS_PER_LESSON = 20;
    private static final long ANKI2_PLACEHOLDER_SIZE_THRESHOLD = 100000;
    private static final int ANKI21B_SIZE_MULTIPLIER = 2;
    private static final String NOTES_QUERY = """
            SELECT flds as fields
            FROM notes
            LIMIT 10000
            """;
    private static final int BATCH_SIZE = 1000;
    private static final String DEFAULT_TOPIC = "Default";
    private static final String DEFAULT_PLACEHOLDER = "-";
    private static final String COURSE_TYPE = "Custom";

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;

    @Transactional
    @EvictAllCaches
    public Map<String, Object> importAnkiFile(MultipartFile file, String displayName, User owner) throws Exception {
        File tempDir = null;
        try {
            tempDir = extractApkgToTempDir(file);
            File collectionFile = findCollectionDatabase(tempDir);
            if (collectionFile == null) {
                throw new IllegalArgumentException("Invalid Anki deck: collection database not found");
            }

            ParseResult parseResult = parseAnkiDatabase(collectionFile);
            if (parseResult.items().isEmpty()) {
                return Map.of("message", "No valid text cards found", "skippedItems", parseResult.skippedItems());
            }

            String courseName = displayName != null ? displayName.replace(".apkg", "").trim() : "Imported Course";
            if (courseName.isEmpty()) {
                courseName = "Imported Course";
            }

            deletePreviousCourses(courseName, owner);

            Course course = new Course(courseName, "Imported from Anki deck", COURSE_TYPE);
            course.setOwner(owner);
            course = courseRepository.save(course);

            Map<String, List<AnkiItem>> itemsByTopic = groupByTopic(parseResult.items());
            int itemsCreated = saveItemsToDatabase(itemsByTopic, course);

            log.info("Imported {} items from {}", itemsCreated, displayName);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Import successful");
            result.put("coursesCreated", 1);
            result.put("courseId", course.getId());
            result.put("courseName", course.getTitle());
            result.put("topicsCreated", itemsByTopic.size());
            result.put("itemsCreated", itemsCreated);
            result.put("skippedItems", parseResult.skippedItems());
            result.put("warnings", parseResult.warnings());
            return result;
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private File extractApkgToTempDir(MultipartFile file) throws Exception {
        File tempDir = Files.createTempDirectory("anki-import").toFile();
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File extractedFile = new File(tempDir, entry.getName());
                if (!entry.isDirectory()) {
                    extractedFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(extractedFile)) {
                        byte[] buffer = new byte[ZIP_BUFFER_SIZE];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
        return tempDir;
    }

    private File findCollectionDatabase(File tempDir) {
        File anki2File = new File(tempDir, "collection.anki2");
        File anki21File = new File(tempDir, "collection.anki21");
        File anki21bFile = new File(tempDir, "collection.anki21b");

        if (anki21bFile.exists()) {
            long anki21bSize = anki21bFile.length();
            long anki2Size = anki2File.exists() ? anki2File.length() : 0;
            if (anki21bSize > anki2Size * ANKI21B_SIZE_MULTIPLIER || anki2Size < ANKI2_PLACEHOLDER_SIZE_THRESHOLD) {
                File decompressedFile = new File(tempDir, "collection_decompressed.anki2");
                try {
                    decompressZstd(anki21bFile, decompressedFile);
                    return decompressedFile;
                } catch (java.io.IOException e) {
                    log.error("Failed to decompress .anki21b: {}", e.getMessage());
                }
            }
        }

        if (anki2File.exists()) {
            return anki2File;
        }
        if (anki21File.exists()) {
            return anki21File;
        }

        File[] files = tempDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".anki21b")) {
                    File decompressedFile = new File(tempDir, f.getName().replace(".anki21b", "_decompressed.anki2"));
                    try {
                        decompressZstd(f, decompressedFile);
                        return decompressedFile;
                    } catch (java.io.IOException e) {
                        log.error("Failed to decompress {}: {}", f.getName(), e.getMessage());
                    }
                }
            }
            for (File f : files) {
                if (f.getName().endsWith(".anki2")) {
                    return f;
                }
            }
            for (File f : files) {
                if (f.getName().endsWith(".anki21")) {
                    return f;
                }
            }
        }
        return null;
    }

    private void decompressZstd(File input, File output) throws java.io.IOException {
        try (FileInputStream fis = new FileInputStream(input); ZstdInputStream zis = new ZstdInputStream(fis); FileOutputStream fos = new FileOutputStream(output)) {
            byte[] buffer = new byte[ZSTD_BUFFER_SIZE];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    private ParseResult parseAnkiDatabase(File collectionFile) throws Exception {
        List<AnkiItem> items = new ArrayList<>();
        int skippedItems = 0;
        List<String> warnings = new ArrayList<>();

        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:" + collectionFile.getAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {
            List<String> fieldNames = getFieldNamesFromModels(conn);

            try (ResultSet rs = stmt.executeQuery(NOTES_QUERY)) {
                while (rs.next()) {
                    String fields = rs.getString("fields");
                    if (fields == null || fields.trim().isEmpty()
                            || fields.contains("Please update to the latest Anki version")) {
                        skippedItems++;
                        continue;
                    }

                    String[] parts = fields.split(ANKI_FIELD_DELIMITER_REGEX);
                    String expression = parts.length > 0 ? cleanText(parts[0]) : "";
                    String reading = parts.length > 1 ? cleanText(parts[1]) : "";
                    String meaning = parts.length > 2 ? cleanText(parts[2]) : "";

                    if (expression.trim().isEmpty()) {
                        for (String part : parts) {
                            String cleaned = cleanText(part);
                            if (!cleaned.trim().isEmpty()) {
                                expression = cleaned;
                                break;
                            }
                        }
                    }

                    if (expression.trim().isEmpty() && meaning.trim().isEmpty()) {
                        skippedItems++;
                        continue;
                    }

                    AnkiItem item = new AnkiItem();
                    item.setFront(expression.length() <= MAX_EXPRESSION_LENGTH ? expression : expression.substring(0, MAX_EXPRESSION_LENGTH));
                    item.setReading(reading.isEmpty() ? null : (reading.length() <= MAX_READING_LENGTH ? reading : reading.substring(0, MAX_READING_LENGTH)));
                    item.setBack(meaning.length() <= MAX_MEANING_LENGTH ? meaning : meaning.substring(0, MAX_MEANING_LENGTH));
                    item.setFields(buildFieldsMap(parts, fieldNames));
                    item.setTopic(String.format("Lesson %02d", (items.size() / ITEMS_PER_LESSON) + 1));

                    items.add(item);

                    if (expression.length() > MAX_EXPRESSION_LENGTH || meaning.length() > MAX_MEANING_LENGTH) {
                        if (!warnings.contains(WARN_TRUNCATION)) {
                            warnings.add(WARN_TRUNCATION);
                        }
                    }
                }
            }
        }

        log.info("Parsed {} cards, skipped {}", items.size(), skippedItems);
        return new ParseResult(items, skippedItems, warnings);
    }

    private Map<String, List<AnkiItem>> groupByTopic(List<AnkiItem> items) {
        Map<String, List<AnkiItem>> itemsByTopic = new TreeMap<>((a, b) -> {
            String left = a == null ? "" : a;
            String right = b == null ? "" : b;
            java.util.regex.Matcher mA = java.util.regex.Pattern.compile("\\d+").matcher(left);
            java.util.regex.Matcher mB = java.util.regex.Pattern.compile("\\d+").matcher(right);
            int numA = mA.find() ? Integer.parseInt(mA.group()) : Integer.MAX_VALUE;
            int numB = mB.find() ? Integer.parseInt(mB.group()) : Integer.MAX_VALUE;
            return numA != numB ? numA - numB : left.compareToIgnoreCase(right);
        });
        for (AnkiItem item : items) {
            String topicName = item.getTopic() != null ? item.getTopic() : DEFAULT_TOPIC;
            itemsByTopic.computeIfAbsent(topicName, k -> new ArrayList<>()).add(item);
        }
        return itemsByTopic;
    }

    private int saveItemsToDatabase(Map<String, List<AnkiItem>> itemsByTopic, Course course) {
        int topicOrder = 0;
        int itemsCreated = 0;

        for (Map.Entry<String, List<AnkiItem>> entry : itemsByTopic.entrySet()) {
            Topic topic = new Topic();
            topic.setTitle(entry.getKey());
            topic.setCourse(course);
            topic.setOrderIndex(topicOrder++);
            topic = topicRepository.save(topic);

            List<StudyItem> batch = new ArrayList<>();
            for (AnkiItem ankiItem : entry.getValue()) {
                StudyItem studyItem = createStudyItem(ankiItem, topic);
                batch.add(studyItem);
                if (batch.size() >= BATCH_SIZE) {
                    studyItemRepository.saveAll(batch);
                    studyItemRepository.flush();
                    itemsCreated += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                studyItemRepository.saveAll(batch);
                studyItemRepository.flush();
                itemsCreated += batch.size();
                batch.clear();
            }
        }
        return itemsCreated;
    }

    private StudyItem createStudyItem(AnkiItem ankiItem, Topic topic) {
        StudyItem studyItem = new StudyItem();
        Map<String, String> fields = ankiItem.getFields() != null ? ankiItem.getFields() : new HashMap<>();
        studyItem.setAdditionalData(fields);

        String primary = null;
        for (String fn : new String[]{"Expression", "Kanji", "Front"}) {
            String v = fields.get(fn);
            if (v != null && !v.isBlank()) {
                primary = v;
                break;
            }
        }
        studyItem.setPrimaryText(primary != null && !primary.isBlank() ? primary : (ankiItem.getFront() != null && !ankiItem.getFront().isBlank() ? ankiItem.getFront() : DEFAULT_PLACEHOLDER));

        String secondary = null;
        for (String fn : new String[]{"Reading", "Kana", "Furigana"}) {
            String v = fields.get(fn);
            if (v != null && !v.isBlank()) {
                secondary = v;
                break;
            }
        }
        studyItem.setSecondaryText(secondary != null && !secondary.isBlank() ? secondary : (ankiItem.getReading() != null && !ankiItem.getReading().isBlank() ? ankiItem.getReading() : DEFAULT_PLACEHOLDER));

        String meaning = null;
        for (String fn : new String[]{"Meaning", "English", "Back"}) {
            String v = fields.get(fn);
            if (v != null && !v.isBlank()) {
                meaning = v;
                break;
            }
        }
        studyItem.setMeaning(meaning != null && !meaning.isBlank() ? meaning : (ankiItem.getBack() != null && !ankiItem.getBack().isBlank() ? ankiItem.getBack() : DEFAULT_PLACEHOLDER));

        studyItem.setTopic(topic);
        return studyItem;
    }

    private void deletePreviousCourses(String courseName, User owner) {
        List<Course> existingCourses = owner == null
                ? courseRepository.findByTitle(courseName)
                : courseRepository.findByTitleAndOwner_Id(courseName, owner.getId());
        courseRepository.deleteAll(existingCourses);
        courseRepository.flush();
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        text = text.replaceAll("\\[sound:[^\\]]+\\]", "")
                .replaceAll("<img[^>]+>", "")
                .replaceAll("\\[anki:play:[^\\]]+\\]", "")
                .replaceAll("<[^>]+>", "");
        return text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void deleteDirectory(File directory) {
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

    private List<String> getFieldNamesFromModels(Connection conn) {
        List<String> fieldNames = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            String modelsCol = null;
            try (ResultSet rs = stmt.executeQuery("SELECT models FROM col LIMIT 1")) {
                if (rs.next()) {
                    modelsCol = rs.getString("models");
                }
            }
            if (modelsCol != null && !modelsCol.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(modelsCol);
                for (JsonNode model : root) {
                    JsonNode flds = model.get("flds");
                    if (flds != null && flds.isArray()) {
                        for (JsonNode fld : flds) {
                            JsonNode nameNode = fld.get("name");
                            if (nameNode != null) {
                                fieldNames.add(nameNode.asText());
                            }
                        }
                        if (!fieldNames.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        } catch (java.io.IOException | SQLException e) {
            log.warn("Could not parse field names from models: {}", e.getMessage());
        }
        return fieldNames;
    }

    private Map<String, String> buildFieldsMap(String[] parts, List<String> fieldNames) {
        Map<String, String> fieldsMap = new HashMap<>();
        for (int i = 0; i < parts.length; i++) {
            String cleaned = cleanText(parts[i]);
            if (!cleaned.isEmpty()) {
                String fieldName = i < fieldNames.size() ? fieldNames.get(i) : "Field" + (i + 1);
                fieldsMap.put(fieldName, cleaned);
            }
        }
        return fieldsMap;
    }

    public record ParseResult(List<AnkiItem> items, int skippedItems, List<String> warnings) {

    }
}
