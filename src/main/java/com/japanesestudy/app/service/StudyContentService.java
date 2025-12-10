package com.japanesestudy.app.service;

import com.japanesestudy.app.repository.*;
import com.japanesestudy.app.model.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.PageRequest;

@Service
@Transactional
public class StudyContentService {
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final ItemTypeRepository itemTypeRepository;

    public StudyContentService(CourseRepository courseRepository,
            TopicRepository topicRepository,
            StudyItemRepository studyItemRepository,
            ItemTypeRepository itemTypeRepository) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.studyItemRepository = studyItemRepository;
        this.itemTypeRepository = itemTypeRepository;
    }

    @Cacheable("courses")
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<Topic> getTopicsByCourse(Long courseId) {
        // Optimized: use indexed query instead of filtering all
        return topicRepository.findByCourseId(courseId);
    }

    public Topic createTopic(Topic topic) {
        if (topic.getOrderIndex() == null && topic.getCourse() != null) {
            List<Topic> existingTopics = getTopicsByCourse(topic.getCourse().getId());
            int maxIndex = existingTopics.stream()
                    .mapToInt(t -> t.getOrderIndex() != null ? t.getOrderIndex() : 0)
                    .max()
                    .orElse(0);
            topic.setOrderIndex(maxIndex + 1);
        }
        return topicRepository.save(topic);
    }

    public Topic getTopicByTitle(String title) {
        return topicRepository.findByTitleIgnoreCase(title).orElse(null);
    }

    @Cacheable(value = "items", key = "#topicId")
    @Transactional(readOnly = true)
    public List<StudyItem> getItemsByTopic(Long topicId) {
        // Optimized: use indexed query instead of filtering all
        return studyItemRepository.findByTopicId(topicId);
    }

    @CacheEvict(value = "items", allEntries = true)
    public StudyItem createStudyItem(StudyItem item) {
        return studyItemRepository.save(item);
    }

    @CacheEvict(value = "items", allEntries = true)
    public List<StudyItem> createStudyItems(List<StudyItem> items) {
        return studyItemRepository.saveAll(items);
    }

    @CacheEvict(value = "items", allEntries = true)
    public StudyItem updateStudyItem(Long id, StudyItem updatedItem) {
        StudyItem item = studyItemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found"));
        item.setPrimaryText(updatedItem.getPrimaryText());
        item.setSecondaryText(updatedItem.getSecondaryText());
        item.setMeaning(updatedItem.getMeaning());
        return studyItemRepository.save(item);
    }

    public void deleteStudyItem(Long id) {
        studyItemRepository.deleteById(id);
    }

    public List<StudyItem> getDailyReviewCards(Long userId, int count) {
        // Optimized: single query with random order
        return studyItemRepository.findRandom(PageRequest.of(0, count));
    }

    @Cacheable("itemTypes")
    public List<ItemType> getAllItemTypes() {
        return itemTypeRepository.findAll();
    }

    public ItemType getItemTypeByName(String name) {
        return itemTypeRepository.findByNameIgnoreCase(name).orElse(null);
    }
}
