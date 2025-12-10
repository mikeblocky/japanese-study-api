package com.japanesestudy.app.service;

import com.japanesestudy.app.model.Course;
import com.japanesestudy.app.model.StudyItem;
import com.japanesestudy.app.model.Topic;
import com.japanesestudy.app.model.ItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class AnkiImportService {

    @Autowired
    private StudyContentService contentService;

    public void importAnkiPackage(String apkgPath) {
        System.out.println("Starting Anki Import from: " + apkgPath);
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "anki_import_" + System.currentTimeMillis());
        if (!tempDir.mkdirs()) {
            System.err.println("Failed to create temp directory");
            return;
        }

        try (ZipFile zipFile = new ZipFile(apkgPath)) {
            // 1. Unzip 'collection.anki2'
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            File collectionFile = null;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals("collection.anki2")) {
                    collectionFile = new File(tempDir, "collection.anki2");
                    try (InputStream is = zipFile.getInputStream(entry);
                            FileOutputStream fos = new FileOutputStream(collectionFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                    break;
                }
            }

            if (collectionFile == null) {
                System.err.println("collection.anki2 not found in .apkg");
                return;
            }

            // 2. Read SQLite DB
            String url = "jdbc:sqlite:" + collectionFile.getAbsolutePath();
            try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt = conn.createStatement()) {

                ItemType typeVocab = contentService.getItemTypeByName("VOCABULARY"); // Assumes initialized

                // Read notes with tags
                ResultSet rs = stmt.executeQuery("SELECT flds, tags FROM notes");
                int successCount = 0;

                while (rs.next()) {
                    String flds = rs.getString("flds");
                    String tags = rs.getString("tags");

                    // Anki fields are separated by 0x1F (Unit Separator)
                    String[] parts = flds.split("\u001f");

                    if (parts.length >= 2) {
                        // The last field contains the lesson number (e.g., "1", "2", etc.)
                        Topic topic = null;
                        String lastField = parts[parts.length - 1].trim();

                        // Try to parse lesson number from last field
                        try {
                            int lessonNum = Integer.parseInt(lastField);
                            topic = contentService.getTopicByTitle("Lesson " + lessonNum);
                        } catch (NumberFormatException e) {
                            // Last field is not a number, try from tags as fallback
                            if (tags != null && !tags.isEmpty()) {
                                java.util.regex.Matcher m = java.util.regex.Pattern.compile("Lesson[_\\s]?(\\d+)")
                                        .matcher(tags);
                                if (m.find()) {
                                    String lessonNumStr = m.group(1);
                                    topic = contentService.getTopicByTitle("Lesson " + lessonNumStr);
                                }
                            }
                        }

                        // Skip if no topic found
                        if (topic == null) {
                            continue;
                        }

                        StudyItem item = new StudyItem();
                        item.setPrimaryText(parts[0]); // Japanese word
                        item.setSecondaryText(parts.length > 2 ? parts[2] : ""); // Reading (furigana)
                        item.setMeaning(parts.length > 1 ? parts[1] : ""); // English meaning
                        item.setTopic(topic);
                        item.setType(typeVocab);
                        contentService.createStudyItem(item);
                        successCount++;
                    }
                }
                System.out.println("Anki Import Completed. Imported " + successCount + " items mapped to lessons.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
