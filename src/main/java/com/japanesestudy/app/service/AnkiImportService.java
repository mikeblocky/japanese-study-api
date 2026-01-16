package com.japanesestudy.app.service;

import com.japanesestudy.app.dto.importing.AnkiImportRequest;
import com.japanesestudy.app.dto.importing.AnkiItem;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AnkiImportService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;

    @Transactional(timeout = 300)
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Map<String, Object> importAnki(AnkiImportRequest request) {
        if (request == null || request.getItems().isEmpty()) {
            Map<String, Object> empty = new java.util.HashMap<>();
            empty.put("message", "No items provided");
            empty.put("courseId", 0);
            empty.put("topicsCreated", 0);
            empty.put("itemsCreated", 0);
            return empty;
        }

        String courseName = request.getCourseName() != null ? request.getCourseName() : "Imported Course";
        List<Course> existingCourses = courseRepository.findByTitle(courseName);
        for (Course existing : existingCourses) {
            courseRepository.delete(existing);
        }
        if (!existingCourses.isEmpty()) courseRepository.flush();

        Course course = new Course(courseName, request.getDescription(), "Custom");
        course = courseRepository.save(course);

        Map<String, List<AnkiItem>> itemsByTopic = new TreeMap<>((a, b) -> {
            int numA = extractNumber(a);
            int numB = extractNumber(b);
            if (numA != numB) return numA - numB;
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
                if (primaryText == null || primaryText.isBlank()) primaryText = "-";

                String secondaryText = fields.getOrDefault("Reading",
                    fields.getOrDefault("Kana",
                    fields.getOrDefault("Furigana", ankiItem.getReading())));
                if (secondaryText == null || secondaryText.isBlank()) secondaryText = primaryText;

                String meaning = fields.getOrDefault("Meaning",
                    fields.getOrDefault("English",
                    fields.getOrDefault("Back", ankiItem.getBack())));
                if (meaning == null || meaning.isBlank()) meaning = "-";

                studyItem.setPrimaryText(primaryText);
                studyItem.setSecondaryText(secondaryText);
                studyItem.setMeaning(meaning);
                studyItem.setTopic(topic);
                studyItem.setType("VOCABULARY");
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
        if (title == null) return Integer.MAX_VALUE;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(title);
        return m.find() ? Integer.parseInt(m.group()) : Integer.MAX_VALUE;
    }
}
