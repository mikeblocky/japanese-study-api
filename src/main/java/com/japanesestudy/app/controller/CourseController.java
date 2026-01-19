package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.CatalogService;
import com.japanesestudy.app.service.CatalogService.CourseSummary;
import static com.japanesestudy.app.util.Utils.created;
import static com.japanesestudy.app.util.Utils.getOrThrow;
import static com.japanesestudy.app.util.Utils.noContent;
import static com.japanesestudy.app.util.Utils.ok;
import static com.japanesestudy.app.util.Utils.validateOwnership;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CatalogService catalogService;

    @GetMapping("/{courseId}/topics")
    public ResponseEntity<List<Topic>> getTopicsForCourse(@PathVariable Long courseId) {
        return ok(catalogService.getTopicsByCourse(courseId));
    }

    @PostMapping("/{courseId}/topics")
    public ResponseEntity<Topic> createTopicForCourse(
            @PathVariable Long courseId,
            @RequestBody Topic topic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        validateOwnership(course.getOwner() != null ? course.getOwner().getId() : null, userDetails.getId());

        topic.setCourse(course);
        if (topic.getOrderIndex() == null) {
            topic.setOrderIndex(catalogService.getTopicsByCourse(courseId).size());
        }

        return created(catalogService.createTopic(topic));
    }

    @GetMapping("/{courseId}/summary")
    public ResponseEntity<CourseSummary> getCourseSummary(@PathVariable Long courseId) {
        return ok(catalogService.getCourseSummary(courseId));
    }

    @PostMapping("/{courseId}/topics/reorder")
    public ResponseEntity<Integer> reorderTopicsAlphabetically(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        validateOwnership(course.getOwner() != null ? course.getOwner().getId() : null, userDetails.getId());
        int count = catalogService.reorderTopicsByTitle(courseId);
        return ok(count);
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ok(catalogService.getAllCourses());
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long courseId) {
        return catalogService.getCourseById(courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(
            @RequestBody Course course,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User owner = new User();
        owner.setId(userDetails.getId());
        course.setOwner(owner);
        return created(catalogService.createCourse(course));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<Course> updateCourse(
            @PathVariable Long courseId,
            @RequestBody Course updates,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        validateOwnership(course.getOwner() != null ? course.getOwner().getId() : null, userDetails.getId());

        // Manual field updates for clarity
        if (updates.getTitle() != null) {
            course.setTitle(updates.getTitle());
        }
        if (updates.getDescription() != null) {
            course.setDescription(updates.getDescription());
        }
        if (updates.getMinLevel() != null) {
            course.setMinLevel(updates.getMinLevel());
        }
        if (updates.getMaxLevel() != null) {
            course.setMaxLevel(updates.getMaxLevel());
        }
        if (updates.getTags() != null) {
            course.setTags(updates.getTags());
        }
        if (updates.getCategory() != null) {
            course.setCategory(updates.getCategory());
        }
        if (updates.getDifficulty() != null) {
            course.setDifficulty(updates.getDifficulty());
        }
        if (updates.getEstimatedHours() != null) {
            course.setEstimatedHours(updates.getEstimatedHours());
        }

        return ok(catalogService.updateCourse(course));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        validateOwnership(course.getOwner() != null ? course.getOwner().getId() : null, userDetails.getId());
        catalogService.deleteCourse(courseId);
        return noContent();
    }
}
