package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CatalogService catalogService;

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
    public ResponseEntity<List<Topic>> getTopicsByCourse(@PathVariable long id) {
        return catalogService.getCourseById(id)
                .map(course -> ResponseEntity.ok(catalogService.getTopicsByCourse(id)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/topics")
    public ResponseEntity<Topic> createTopicForCourse(@PathVariable long id, @RequestBody Topic topic) {
        return catalogService.getCourseById(id)
                .map(course -> {
                    topic.setCourse(course);
                    return ResponseEntity.ok(catalogService.createTopic(topic));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(catalogService.createCourse(course));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable long id, @RequestBody Course course) {
        return catalogService.getCourseById(id)
                .map(existing -> {
                    existing.setTitle(course.getTitle());
                    existing.setDescription(course.getDescription());
                    existing.setLevel(course.getLevel());
                    existing.setMinLevel(course.getMinLevel());
                    existing.setMaxLevel(course.getMaxLevel());
                    existing.setCategory(course.getCategory());
                    existing.setDifficulty(course.getDifficulty());
                    existing.setEstimatedHours(course.getEstimatedHours());
                    existing.setTags(course.getTags());
                    return ResponseEntity.ok(catalogService.updateCourse(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable long id) {
        return catalogService.getCourseById(id)
                .map(existing -> {
                    catalogService.deleteCourse(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reorder-topics")
    public ResponseEntity<?> reorderTopics(@PathVariable long id) {
        int count = catalogService.reorderTopicsByTitle(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Topics reordered", "count", count));
    }
}
