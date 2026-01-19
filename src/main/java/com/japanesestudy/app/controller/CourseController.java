package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.japanesestudy.app.util.Utils.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CatalogService catalogService;

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
        if (updates.getTitle() != null) course.setTitle(updates.getTitle());
        if (updates.getDescription() != null) course.setDescription(updates.getDescription());
        if (updates.getMinLevel() != null) course.setMinLevel(updates.getMinLevel());
        if (updates.getMaxLevel() != null) course.setMaxLevel(updates.getMaxLevel());
        if (updates.getTags() != null) course.setTags(updates.getTags());
        if (updates.getCategory() != null) course.setCategory(updates.getCategory());
        if (updates.getDifficulty() != null) course.setDifficulty(updates.getDifficulty());
        if (updates.getEstimatedHours() != null) course.setEstimatedHours(updates.getEstimatedHours());
        
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
