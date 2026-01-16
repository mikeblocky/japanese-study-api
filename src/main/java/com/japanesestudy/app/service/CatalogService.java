package com.japanesestudy.app.service;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final com.japanesestudy.app.repository.UserProgressRepository userProgressRepository;

    @Cacheable(cacheNames = "courses")
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Cacheable(cacheNames = "courseById", key = "#courseId")
    public Optional<Course> getCourseById(long courseId) {
        return courseRepository.findById(courseId);
    }

    @Cacheable(cacheNames = "topicsByCourse", key = "#courseId")
    public List<Topic> getTopicsByCourse(long courseId) {
        return topicRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Cacheable(cacheNames = "itemsByTopic", key = "#topicId")
    public List<StudyItem> getItemsByTopic(long topicId) {
        return studyItemRepository.findByTopicId(topicId);
    }

    public List<StudyItem> getItemsByTopicForUser(long topicId, Long userId) {
        List<StudyItem> items = getItemsByTopic(topicId); // Use cached version
        if (userId == null) return items;

        List<com.japanesestudy.app.entity.UserProgress> progressList = 
            userProgressRepository.findByUserIdAndTopicId(userId, topicId);

        java.util.Map<Long, Integer> intervalMap = progressList.stream()
            .filter(p -> p.getStudyItem() != null)
            .collect(java.util.stream.Collectors.toMap(
                p -> p.getStudyItem().getId(),
                p -> p.getInterval(),
                (v1, v2) -> v1 // In case of duplicates (shouldn't happen)
            ));

        List<StudyItem> freshItems = studyItemRepository.findByTopicId(topicId);
        freshItems.forEach(item -> {
            Integer interval = intervalMap.get(item.getId());
            item.setUserSrsInterval(interval != null ? interval : 0);
        });
        return freshItems;
    }

    @Cacheable(cacheNames = "itemsByTopic", key = "'topic:' + #topicId + ':limit:' + #limit")
    public List<StudyItem> getItemsByTopic(long topicId, int limit) {
        if (limit <= 0) return List.of();
        return studyItemRepository.findByTopicId(topicId, PageRequest.of(0, limit)).getContent();
    }

    @Transactional
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic"}, allEntries = true)
    public void deleteCourse(long courseId) {
        courseRepository.deleteById(courseId);
    }

    @Transactional
    @CacheEvict(cacheNames = {"topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Topic createTopic(Topic topic) {
        return topicRepository.save(topic);
    }

    @Transactional
    @CacheEvict(cacheNames = {"topicsByCourse", "itemsByTopic"}, allEntries = true)
    public Topic updateTopic(Topic topic) {
        return topicRepository.save(topic);
    }

    @Transactional
    @CacheEvict(cacheNames = {"topicsByCourse", "itemsByTopic"}, allEntries = true)
    public void deleteTopic(long topicId) {
        topicRepository.deleteById(topicId);
    }

    public Optional<Topic> getTopicById(long topicId) {
        return topicRepository.findById(topicId);
    }

    @Transactional
    @CacheEvict(cacheNames = {"itemsByTopic"}, allEntries = true)
    public StudyItem createStudyItem(StudyItem item) {
        return studyItemRepository.save(item);
    }

    @Transactional
    @CacheEvict(cacheNames = {"itemsByTopic"}, allEntries = true)
    public StudyItem updateStudyItem(StudyItem item) {
        return studyItemRepository.save(item);
    }

    @Transactional
    @CacheEvict(cacheNames = {"itemsByTopic"}, allEntries = true)
    public void deleteStudyItem(long itemId) {
        studyItemRepository.deleteById(itemId);
    }

    public Optional<StudyItem> getStudyItemById(long itemId) {
        return studyItemRepository.findById(itemId);
    }

    @Transactional
    @CacheEvict(cacheNames = {"topicsByCourse"}, allEntries = true)
    public int reorderTopicsByTitle(long courseId) {
        List<Topic> topics = topicRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        topics.sort((a, b) -> {
            int numA = extractNumber(a.getTitle());
            int numB = extractNumber(b.getTitle());
            if (numA != numB) return numA - numB;
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        });
        for (int i = 0; i < topics.size(); i++) {
            topics.get(i).setOrderIndex(i);
        }
        topicRepository.saveAll(topics);
        return topics.size();
    }

    private int extractNumber(String title) {
        if (title == null) return Integer.MAX_VALUE;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(title);
        if (m.find()) return Integer.parseInt(m.group());
        return Integer.MAX_VALUE;
    }
}
