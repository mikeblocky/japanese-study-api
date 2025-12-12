package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.service.CatalogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CatalogService catalogService;
    private final CourseRepository courseRepository;

    public CourseController(CatalogService catalogService, CourseRepository courseRepository) {
        this.catalogService = catalogService;
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return catalogService.getAllCourses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable long id) {
        return catalogService.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/topics")
    public List<Topic> getTopicsByCourse(@PathVariable long id) {
        return catalogService.getTopicsByCourse(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public Course createCourse(@Valid @NonNull @RequestBody Course course) {
        return courseRepository.save(course);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public ResponseEntity<?> deleteCourse(@PathVariable long id) {
        if (!courseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        courseRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
