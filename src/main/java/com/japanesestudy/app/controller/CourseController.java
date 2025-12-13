package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<Topic> getTopicsByCourse(@PathVariable long id) {
        return catalogService.getTopicsByCourse(id);
    }

    @PostMapping("/{id}/topics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Topic> createTopicForCourse(@PathVariable long id, @RequestBody Topic topic) {
        return catalogService.getCourseById(id)
                .map(course -> {
                    topic.setCourse(course);
                    Topic created = catalogService.createTopic(topic);
                    return ResponseEntity.ok(created);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Course created = catalogService.createCourse(course);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable long id, @RequestBody Course course) {
        return catalogService.getCourseById(id)
                .map(existing -> {
                    // Only update the fields that should change, preserve relationships
                    existing.setTitle(course.getTitle());
                    existing.setDescription(course.getDescription());
                    // Topics are preserved since we're updating the existing entity
                    return ResponseEntity.ok(catalogService.updateCourse(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable long id) {
        catalogService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reorder-topics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reorderTopics(@PathVariable long id) {
        int count = catalogService.reorderTopicsByTitle(id);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Topics reordered successfully",
                "topicsReordered", count
        ));
    }
}
