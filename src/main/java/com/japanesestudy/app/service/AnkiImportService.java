package com.japanesestudy.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.dto.importing.AnkiImportRequest;
import com.japanesestudy.app.dto.importing.AnkiItem;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnkiImportService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;

    @Transactional(timeout = 300)
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Map<String, Object> importAnki(AnkiImportRequest request, com.japanesestudy.app.entity.User owner) {
        return importAnki(request, owner, java.util.Collections.emptyMap());
    }

    /**
     * Import Anki deck with media URL mappings.
     *
     * @param mediaUrls Map of original filename -> stored URL for media files
     */
    @Transactional(timeout = 300)
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Map<String, Object> importAnki(AnkiImportRequest request, com.japanesestudy.app.entity.User owner, Map<String, String> mediaUrls) {
        System.out.println("DEBUG: AnkiImportService.importAnki() called with " + (request != null ? request.getItems().size() : 0) + " items");
        if (request == null || request.getItems().isEmpty()) {
            Map<String, Object> empty = new java.util.HashMap<>();
            empty.put("message", "No items provided");
            empty.put("courseId", 0);
            empty.put("topicsCreated", 0);
            empty.put("itemsCreated", 0);
            return empty;
        }

        String courseName = request.getCourseName() != null ? request.getCourseName() : "Imported Course";
        // Remove prior courses for this owner with the same title; keep other users' data intact
        List<Course> existingCourses;
        if (owner == null) {
            existingCourses = courseRepository.findByTitle(courseName);
        } else {
            existingCourses = courseRepository.findByTitleAndOwnerId(courseName, owner.getId());
        }
        courseRepository.deleteAll(existingCourses);
        courseRepository.flush();

        Course course = new Course(courseName, request.getDescription(), "Custom");
        course.setOwner(owner);
        course = courseRepository.save(course);

        Map<String, List<AnkiItem>> itemsByTopic = new TreeMap<>((a, b) -> {
            int numA = extractNumber(a);
            int numB = extractNumber(b);
            if (numA != numB) {
                return numA - numB;
            }
            return a.compareToIgnoreCase(b);
        });

        for (AnkiItem item : request.getItems()) {
            String topicName = item.getTopic() != null ? item.getTopic() : "Default";
            itemsByTopic.computeIfAbsent(topicName, k -> new ArrayList<>()).add(item);
        }

        int topicOrder = 0;
        int itemsCreated = 0;
        final int batchSize = 1000;

        for (Map.Entry<String, List<AnkiItem>> entry : itemsByTopic.entrySet()) {
            Topic topic = new Topic();
            topic.setTitle(entry.getKey());
            topic.setCourse(course);
            topic.setOrderIndex(topicOrder++);
            topic = topicRepository.save(topic);

            List<StudyItem> batch = new ArrayList<>();
            for (AnkiItem ankiItem : entry.getValue()) {
                StudyItem studyItem = new StudyItem();
                Map<String, String> fields = ankiItem.getFields() != null ? ankiItem.getFields() : new java.util.HashMap<>();
                studyItem.setAdditionalData(fields);

                String primaryText = fields.getOrDefault("Expression",
                        fields.getOrDefault("Kanji",
                                fields.getOrDefault("Front", ankiItem.getFront())));
                if (primaryText == null || primaryText.isBlank()) {
                    primaryText = "-";
                }

                String secondaryText = fields.getOrDefault("Reading",
                        fields.getOrDefault("Kana",
                                fields.getOrDefault("Furigana", ankiItem.getReading())));
                if (secondaryText == null || secondaryText.isBlank()) {
                    secondaryText = primaryText;
                }

                String meaning = fields.getOrDefault("Meaning",
                        fields.getOrDefault("English",
                                fields.getOrDefault("Back", ankiItem.getBack())));
                if (meaning == null || meaning.isBlank()) {
                    meaning = "-";
                }

                studyItem.setPrimaryText(primaryText);
                studyItem.setSecondaryText(secondaryText);
                studyItem.setMeaning(meaning);
                studyItem.setTopic(topic);

                // Extract media URLs from item fields
                if (!mediaUrls.isEmpty()) {
                    extractAndSetMedia(studyItem, ankiItem, mediaUrls);
                }

                batch.add(studyItem);

                if (batch.size() >= batchSize) {
                    itemsCreated += persistBatch(batch);
                }
            }
            if (!batch.isEmpty()) {
                itemsCreated += persistBatch(batch);
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("message", "Import successful");
        result.put("courseId", course.getId());
        result.put("courseName", course.getTitle());
        result.put("topicsCreated", itemsByTopic.size());
        result.put("itemsCreated", itemsCreated);
        return result;
    }

    private int persistBatch(List<StudyItem> batch) {
        studyItemRepository.saveAll(batch);
        studyItemRepository.flush();
        int size = batch.size();
        batch.clear();
        return size;
    }

    private int extractNumber(String title) {
        if (title == null) {
            return Integer.MAX_VALUE;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(title);
        return m.find() ? Integer.parseInt(m.group()) : Integer.MAX_VALUE;
    }

    /**
     * Extracts media references from AnkiItem fields and sets URLs on
     * StudyItem.
     */
    private void extractAndSetMedia(StudyItem studyItem, AnkiItem ankiItem, Map<String, String> mediaUrls) {
        StringBuilder audioUrls = new StringBuilder();
        StringBuilder imageUrls = new StringBuilder();

        // Combine all text fields to search for media references
        String allText = String.join(" ",
                ankiItem.getFront() != null ? ankiItem.getFront() : "",
                ankiItem.getBack() != null ? ankiItem.getBack() : "",
                ankiItem.getReading() != null ? ankiItem.getReading() : ""
        );

        if (ankiItem.getFields() != null) {
            for (String value : ankiItem.getFields().values()) {
                allText += " " + (value != null ? value : "");
            }
        }

        // Find [sound:filename] references
        java.util.regex.Pattern soundPattern = java.util.regex.Pattern.compile("\\[sound:([^\\]]+)\\]");
        java.util.regex.Matcher soundMatcher = soundPattern.matcher(allText);
        while (soundMatcher.find()) {
            String filename = soundMatcher.group(1);
            String url = mediaUrls.get(filename);
            if (url != null) {
                if (audioUrls.length() > 0) {
                    audioUrls.append(",");
                }
                audioUrls.append(url);
            }
        }

        // Find <img src="filename"> references
        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile("<img[^>]*src=[\"']?([^\"'>\\s]+)[\"']?");
        java.util.regex.Matcher imgMatcher = imgPattern.matcher(allText);
        while (imgMatcher.find()) {
            String filename = imgMatcher.group(1);
            String url = mediaUrls.get(filename);
            if (url != null) {
                if (imageUrls.length() > 0) {
                    imageUrls.append(",");
                }
                imageUrls.append(url);
            }
        }

        if (audioUrls.length() > 0) {
            studyItem.setAudioUrl(audioUrls.toString());
        }
        if (imageUrls.length() > 0) {
            studyItem.setImageUrl(imageUrls.toString());
        }
    }
}
