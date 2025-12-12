package com.japanesestudy.app.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;

@Service
public class CatalogService {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;

    public CatalogService(
            CourseRepository courseRepository,
            TopicRepository topicRepository,
            StudyItemRepository studyItemRepository) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.studyItemRepository = studyItemRepository;
    }

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

    @Cacheable(cacheNames = "itemsByTopic", key = "'topic:' + #topicId + ':limit:' + #limit")
    public List<StudyItem> getItemsByTopic(long topicId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return studyItemRepository.findByTopicId(topicId, PageRequest.of(0, limit)).getContent();
    }
}
