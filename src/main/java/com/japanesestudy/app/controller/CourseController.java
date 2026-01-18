package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CatalogService catalogService;
    private final UserRepository userRepository;

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
    public ResponseEntity<Topic> createTopicForCourse(
            @PathVariable long id,
            @RequestBody Topic topic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return catalogService.getCourseById(id)
                .map(course -> {
                    // Only owner can add topics
                    if (!isOwner(course, userDetails)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Topic>build();
                    }
                    topic.setCourse(course);
                    return ResponseEntity.ok(catalogService.createTopic(topic));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(
            @RequestBody Course course,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Set owner to current user
        if (userDetails != null) {
            User owner = userRepository.findById(userDetails.getId()).orElse(null);
            course.setOwner(owner);
        }
        return ResponseEntity.ok(catalogService.createCourse(course));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(
            @PathVariable long id,
            @RequestBody Course course,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return catalogService.getCourseById(id)
                .map(existing -> {
                    // Only owner can update
                    if (!isOwner(existing, userDetails)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Course>build();
                    }
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
    public ResponseEntity<Void> deleteCourse(
            @PathVariable long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return catalogService.getCourseById(id)
                .map(existing -> {
                    // Only owner can delete
                    if (!isOwner(existing, userDetails)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                    }
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

    /**
     * Check if the current user is the owner of the course.
     * Returns true if:
     * - Course has no owner (legacy data), or
     * - Current user is the owner
     */
    private boolean isOwner(Course course, UserDetailsImpl userDetails) {
        if (course.getOwner() == null) {
            return true; // Allow editing legacy courses with no owner
        }
        if (userDetails == null) {
            return false;
        }
        return course.getOwner().getId().equals(userDetails.getId());
    }
}
