package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.CourseAccess;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.entity.AccessLevel;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.CatalogService;
import com.japanesestudy.app.service.CatalogService.CourseSummary;
import com.japanesestudy.app.service.CatalogService.BulkResult;
import com.japanesestudy.app.service.CatalogService.TopicUpsert;
import static com.japanesestudy.app.util.Utils.created;
import static com.japanesestudy.app.util.Utils.getOrThrow;
import static com.japanesestudy.app.util.Utils.noContent;
import static com.japanesestudy.app.util.Utils.ok;

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

    @GetMapping("/{courseId}/topics/page")
    public ResponseEntity<Page<Topic>> getTopicsForCoursePaged(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(catalogService.getTopicsByCourse(courseId, page, size));
    }

    @PostMapping("/{courseId}/topics")
    public ResponseEntity<Topic> createTopicForCourse(
            @PathVariable Long courseId,
            @RequestBody Topic topic,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        requireEditAccess(course.getId(), userDetails);

        topic.setCourse(course);

        return created(catalogService.createTopic(topic, userDetails != null ? userDetails.getId() : null));
    }

    @GetMapping("/{courseId}/summary")
    public ResponseEntity<CourseSummary> getCourseSummary(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ok(catalogService.getCourseSummary(courseId, userId));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Course>> filterCourses(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, name = "q") String query) {
        return ok(catalogService.searchCourses(ownerId, level, tag, query));
    }

    @PostMapping("/{courseId}/topics/reorder")
    public ResponseEntity<Integer> reorderTopicsAlphabetically(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        requireEditAccess(course.getId(), userDetails);
        int count = catalogService.reorderTopicsByTitle(courseId);
        return ok(count);
    }

    @PostMapping("/{courseId}/topics/bulk")
    public ResponseEntity<BulkResult> bulkUpsertTopics(
            @PathVariable Long courseId,
            @RequestBody BulkTopicsRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        requireEditAccess(courseId, userDetails);
        boolean dryRun = request.dryRun() != null && request.dryRun();
        BulkResult result = catalogService.bulkUpsertTopics(courseId, request.topics(), dryRun, userDetails != null ? userDetails.getId() : null);
        return ok(result);
    }

    @PostMapping(value = "/{courseId}/topics/bulk/csv", consumes = "text/csv")
    public ResponseEntity<BulkResult> bulkUpsertTopicsCsv(
            @PathVariable Long courseId,
            @RequestBody String csv,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        requireEditAccess(courseId, userDetails);
        List<TopicUpsert> payload = catalogService.parseTopicsCsv(csv);
        BulkResult result = catalogService.bulkUpsertTopics(courseId, payload, dryRun, userDetails != null ? userDetails.getId() : null);
        return ok(result);
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
        return created(catalogService.createCourse(course, userDetails.getId()));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<Course> updateCourse(
            @PathVariable Long courseId,
            @RequestBody Course updates,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        requireEditAccess(course.getId(), userDetails);

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

        return ok(catalogService.updateCourse(course, userDetails.getId()));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
        requireEditAccess(course.getId(), userDetails);
        catalogService.deleteCourse(courseId, userDetails.getId());
        return noContent();
    }

    @PostMapping("/{courseId}/share")
    public ResponseEntity<CourseAccess> shareCourse(
            @PathVariable Long courseId,
            @RequestBody ShareRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        requireEditAccess(courseId, userDetails);
        AccessLevel level = AccessLevel.valueOf(request.accessLevel().toUpperCase());
        CourseAccess access = catalogService.grantCourseAccess(courseId, request.userId(), level, userDetails.getId(), isAdmin(userDetails));
        return ok(access);
    }

    @DeleteMapping("/{courseId}/share/{userId}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable Long courseId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        requireEditAccess(courseId, userDetails);
        catalogService.revokeCourseAccess(courseId, userId, userDetails.getId(), isAdmin(userDetails));
        return noContent();
    }

    @GetMapping("/{courseId}/share")
    public ResponseEntity<List<CourseAccess>> listShares(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        requireEditAccess(courseId, userDetails);
        return ok(catalogService.listCourseAccess(courseId, userDetails.getId(), isAdmin(userDetails)));
    }

    private void requireEditAccess(Long courseId, UserDetailsImpl userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        boolean admin = isAdmin(userDetails);
        if (!catalogService.canEditCourse(courseId, userId, admin)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    private boolean isAdmin(UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return false;
        }
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public record ShareRequest(Long userId, String accessLevel) {

    }

    public record BulkTopicsRequest(List<TopicUpsert> topics, Boolean dryRun) {

    }
}
