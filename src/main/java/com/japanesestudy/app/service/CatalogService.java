package com.japanesestudy.app.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.util.Utils.EvictAllCaches;
import com.japanesestudy.app.util.Utils.EvictItemCaches;
import com.japanesestudy.app.util.Utils.EvictTopicCaches;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final UserProgressRepository userProgressRepository;

    @Cacheable(cacheNames = "courses")
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Cacheable(cacheNames = "courses", condition = "#userId != null")
    public List<Course> getVisibleCourses(Long userId) {
        return courseRepository.findByOwnerId(userId);
    }

    @Cacheable(cacheNames = "courseById", key = "#courseId")
    public Optional<Course> getCourseById(long courseId) {
        return courseRepository.findById(courseId);
    }

    @Transactional(readOnly = true)
    public CourseSummary getCourseSummary(long courseId) {
        long topics = topicRepository.countByCourseId(courseId);
        long items = studyItemRepository.countByCourseId(courseId);
        return new CourseSummary(topics, items);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "topicsByCourse", key = "#courseId")
    public List<Topic> getTopicsByCourse(long courseId) {
        return topicRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "itemsByTopic", key = "#topicId")
    public List<StudyItem> getItemsByTopic(long topicId) {
        return studyItemRepository.findByTopicId(topicId);
    }

    public List<StudyItem> getItemsByTopicForUser(long topicId, Long userId) {
        List<StudyItem> items = studyItemRepository.findByTopicId(topicId);
        if (userId == null) {
            return items;
        }

        var progressList = userProgressRepository.findByUserIdAndTopicId(userId, topicId);
        var intervalMap = progressList.stream()
                .filter(p -> p.getStudyItem() != null)
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getStudyItem().getId(),
                        com.japanesestudy.app.entity.UserProgress::getInterval,
                        (v1, v2) -> v1
                ));

        items.forEach(item -> item.setUserSrsInterval(intervalMap.getOrDefault(item.getId(), 0)));
        return items;
    }

    @Cacheable(cacheNames = "itemsByTopic", key = "'topic:' + #topicId + ':limit:' + #limit")
    public List<StudyItem> getItemsByTopic(long topicId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return studyItemRepository.findByTopicId(topicId, PageRequest.of(0, limit)).getContent();
    }

    @Transactional
    @EvictAllCaches
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    @EvictAllCaches
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    @EvictAllCaches
    public void deleteCourse(long courseId) {
        courseRepository.deleteById(courseId);
    }

    @Transactional
    @EvictTopicCaches
    public Topic createTopic(Topic topic) {
        return topicRepository.save(topic);
    }

    @Transactional
    @EvictTopicCaches
    public Topic updateTopic(Topic topic) {
        return topicRepository.save(topic);
    }

    @Transactional
    @EvictTopicCaches
    public void deleteTopic(long topicId) {
        topicRepository.deleteById(topicId);
    }

    public Optional<Topic> getTopicById(long topicId) {
        return topicRepository.findByIdWithCourse(topicId);
    }

    @Transactional(readOnly = true)
    public TopicSummary getTopicSummary(long topicId) {
        long items = studyItemRepository.countByTopicId(topicId);
        return new TopicSummary(items);
    }

    @Transactional
    @EvictItemCaches
    public StudyItem createStudyItem(StudyItem item) {
        return studyItemRepository.save(item);
    }

    @Transactional
    @EvictItemCaches
    public StudyItem updateStudyItem(StudyItem item) {
        return studyItemRepository.save(item);
    }

    @Transactional
    @EvictItemCaches
    public void deleteStudyItem(long itemId) {
        studyItemRepository.deleteById(itemId);
    }

    public Optional<StudyItem> getStudyItemById(long itemId) {
        return studyItemRepository.findById(itemId);
    }

    public record CourseSummary(long topics, long items) {

    }

    public record TopicSummary(long items) {

    }

    @Transactional
    @CacheEvict(cacheNames = {"topicsByCourse"}, allEntries = true)
    public int reorderTopicsByTitle(long courseId) {
        List<Topic> topics = topicRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        topics.sort((a, b) -> {
            java.util.regex.Matcher mA = java.util.regex.Pattern.compile("\\d+").matcher(a.getTitle() == null ? "" : a.getTitle());
            java.util.regex.Matcher mB = java.util.regex.Pattern.compile("\\d+").matcher(b.getTitle() == null ? "" : b.getTitle());
            int numA = mA.find() ? Integer.parseInt(mA.group()) : Integer.MAX_VALUE;
            int numB = mB.find() ? Integer.parseInt(mB.group()) : Integer.MAX_VALUE;
            if (numA != numB) {
                return numA - numB;
            }
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        });
        for (int i = 0; i < topics.size(); i++) {
            topics.get(i).setOrderIndex(i);
        }
        topicRepository.saveAll(topics);
        return topics.size();
    }

}
