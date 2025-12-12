package com.japanesestudy.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.dto.AnkiItem;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;

@Service
public class AnkiImportService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;

    public AnkiImportService(
            CourseRepository courseRepository,
            TopicRepository topicRepository,
            StudyItemRepository studyItemRepository) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.studyItemRepository = studyItemRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Map<String, Object> importAnki(AnkiImportRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return Map.of("message", "No items provided", "courseId", null, "topicsCreated", 0, "itemsCreated", 0);
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
                studyItem.setPrimaryText(ankiItem.getFront() != null ? ankiItem.getFront() : "");
                studyItem.setSecondaryText(ankiItem.getReading() != null ? ankiItem.getReading() : "");
                studyItem.setMeaning(ankiItem.getBack() != null ? ankiItem.getBack() : "");
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

        return Map.of(
                "message", "Import successful",
                "courseId", course.getId(),
                "courseName", course.getTitle(),
                "topicsCreated", itemsByTopic.size(),
                "itemsCreated", itemsCreated);
    }

    private int persistBatch(List<StudyItem> batch) {
        studyItemRepository.saveAll(batch);
        studyItemRepository.flush();
        int size = batch.size();
        batch.clear();
        return size;
    }
}
