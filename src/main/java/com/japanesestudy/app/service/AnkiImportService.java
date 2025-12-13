package com.japanesestudy.app.service;

import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.dto.AnkiItem;
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


    @Transactional
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Map<String, Object> importAnki(AnkiImportRequest request) {
        if (request == null || request.getItems().isEmpty()) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("message", "No items provided");
            result.put("courseId", null);
            result.put("topicsCreated", 0);
            result.put("itemsCreated", 0);
            return result;
        }

        Course course = new Course();
        course.setTitle(request.getCourseName() != null ? request.getCourseName() : "Imported Course");
        course.setDescription(request.getDescription());
        course.setLevel("Custom");
        course = courseRepository.save(course);

        Map<String, List<AnkiItem>> itemsByTopic = new TreeMap<>();
        for (AnkiItem item : request.getItems()) {
            String topicName = item.getTopic() != null ? item.getTopic() : "Default";
            itemsByTopic.computeIfAbsent(topicName, k -> new ArrayList<>()).add(item);
        }

        int topicOrder = 0;
        int itemsCreated = 0;
        final int batchSize = 500;
        for (Map.Entry<String, List<AnkiItem>> entry : itemsByTopic.entrySet()) {
            Topic topic = new Topic();
            topic.setTitle(entry.getKey());
            topic.setCourse(course);
            topic.setOrderIndex(topicOrder++);
            topic = topicRepository.save(topic);

            List<StudyItem> batch = new ArrayList<>(Math.min(entry.getValue().size(), batchSize));
            for (AnkiItem ankiItem : entry.getValue()) {
                StudyItem studyItem = new StudyItem();
                String primaryText = ankiItem.getFront() != null && !ankiItem.getFront().trim().isEmpty()
                        ? ankiItem.getFront() : "-";
                String secondaryText = ankiItem.getReading() != null && !ankiItem.getReading().trim().isEmpty()
                        ? ankiItem.getReading() : ankiItem.getFront(); // Fallback to front if no reading
                String meaning = ankiItem.getBack() != null && !ankiItem.getBack().trim().isEmpty()
                        ? ankiItem.getBack() : "-";

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
}
