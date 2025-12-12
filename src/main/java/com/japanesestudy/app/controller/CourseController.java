package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.service.CatalogService;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CatalogService catalogService;

    public CourseController(CatalogService catalogService) {
        this.catalogService = catalogService;
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
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Course created = catalogService.createCourse(course);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable long id, @RequestBody Course course) {
        course.setId(id);
        Course updated = catalogService.updateCourse(course);
        return ResponseEntity.ok(updated);
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
