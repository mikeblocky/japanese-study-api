package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.entity.Visibility;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.service.CatalogService;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Long userId = getCurrentUserId();
        return catalogService.getCoursesForUser(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable long id) {
        Long userId = getCurrentUserId();
        return catalogService.getCourseById(id)
                .filter(course -> canViewCourse(course, userId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/topics")
    public ResponseEntity<List<Topic>> getTopicsByCourse(@PathVariable long id) {
        Long userId = getCurrentUserId();
        return catalogService.getCourseById(id)
                .filter(course -> canViewCourse(course, userId))
                .map(course -> ResponseEntity.ok(catalogService.getTopicsByCourse(id)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/topics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Topic> createTopicForCourse(@PathVariable long id, @RequestBody Topic topic) {
        Long userId = getCurrentUserId();
        return catalogService.getCourseById(id)
                .filter(course -> canModifyCourse(course, userId))
                .map(course -> {
                    topic.setCourse(course);
                    Topic created = catalogService.createTopic(topic);
                    return ResponseEntity.ok(created);
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            userRepository.findById(userId).ifPresent(course::setOwner);
        }
        // Default to PRIVATE if not specified
        if (course.getVisibility() == null) {
            course.setVisibility(Visibility.PRIVATE);
        }
        Course created = catalogService.createCourse(course);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable long id, @RequestBody Course course) {
        Long userId = getCurrentUserId();
        return catalogService.getCourseById(id)
                .filter(existing -> canModifyCourse(existing, userId))
                .map(existing -> {
                    existing.setTitle(course.getTitle());
                    existing.setDescription(course.getDescription());
                    if (course.getVisibility() != null) {
                        existing.setVisibility(course.getVisibility());
                    }
                    // Advanced settings
                    existing.setMinLevel(course.getMinLevel());
                    existing.setMaxLevel(course.getMaxLevel());
                    existing.setTags(course.getTags());
                    existing.setCategory(course.getCategory());
                    existing.setDifficulty(course.getDifficulty());
                    existing.setEstimatedHours(course.getEstimatedHours());
                    return ResponseEntity.ok(catalogService.updateCourse(existing));
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable long id) {
        Long userId = getCurrentUserId();
        return catalogService.getCourseById(id)
                .filter(existing -> canModifyCourse(existing, userId))
                .map(existing -> {
                    catalogService.deleteCourse(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
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

    // Helper: Get current user ID from security context
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) auth.getPrincipal()).getId();
        }
        return null;
    }

    // Helper: Check if user can view a course
    private boolean canViewCourse(Course course, Long userId) {
        if (course.getVisibility() == Visibility.PUBLIC) {
            return true;
        }
        // PRIVATE: only owner can view
        return userId != null && course.getOwner() != null && course.getOwner().getId().equals(userId);
    }

    // Helper: Check if user can modify a course (owner only)
    private boolean canModifyCourse(Course course, Long userId) {
        if (userId == null) return false;
        // Owner can always modify
        if (course.getOwner() != null && course.getOwner().getId().equals(userId)) {
            return true;
        }
        // No owner set (legacy data) - anyone can modify for now
        return course.getOwner() == null;
    }
}

