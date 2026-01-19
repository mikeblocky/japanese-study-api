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

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.CatalogService;
import com.japanesestudy.app.service.CatalogService.BulkResult;
import com.japanesestudy.app.service.CatalogService.StudyItemUpsert;
import com.japanesestudy.app.service.CatalogService.TopicSummary;
import static com.japanesestudy.app.util.Utils.created;
import static com.japanesestudy.app.util.Utils.getOrThrow;
import static com.japanesestudy.app.util.Utils.noContent;
import static com.japanesestudy.app.util.Utils.ok;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final CatalogService catalogService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Topic>> getTopicsByCourse(@PathVariable Long courseId) {
        return ok(catalogService.getTopicsByCourse(courseId));
    }

    @GetMapping("/{topicId}/items")
    public ResponseEntity<List<StudyItem>> getItemsByTopic(@PathVariable Long topicId) {
        return ok(catalogService.getItemsByTopic(topicId));
    }

    @GetMapping("/{topicId}/items/page")
    public ResponseEntity<Page<StudyItem>> getItemsByTopicPaged(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(catalogService.getItemsByTopic(topicId, page, size));
    }

    @PostMapping
    public ResponseEntity<Topic> createTopic(@RequestBody Topic topic) {
        return created(catalogService.createTopic(topic));
    }

    @GetMapping("/{topicId}/summary")
    public ResponseEntity<TopicSummary> getTopicSummary(
            @PathVariable Long topicId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ok(catalogService.getTopicSummary(topicId, userId));
    }

    @PostMapping("/{topicId}/items")
    public ResponseEntity<StudyItem> addItemToTopic(
            @PathVariable Long topicId,
            @RequestBody StudyItem item,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");
        requireEditAccess(topic, userDetails);
        item.setTopic(topic);
        return created(catalogService.createStudyItem(item, userDetails != null ? userDetails.getId() : null));
    }

    @PostMapping("/{topicId}/items/bulk")
    public ResponseEntity<BulkResult> bulkUpsertItems(
            @PathVariable Long topicId,
            @RequestBody BulkItemsRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");
        requireEditAccess(topic, userDetails);
        boolean dryRun = request.dryRun() != null && request.dryRun();
        BulkResult result = catalogService.bulkUpsertStudyItems(topicId, request.items(), dryRun, userDetails != null ? userDetails.getId() : null);
        return ok(result);
    }

    @PostMapping(value = "/{topicId}/items/bulk/csv", consumes = "text/csv")
    public ResponseEntity<BulkResult> bulkUpsertItemsCsv(
            @PathVariable Long topicId,
            @RequestBody String csv,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");
        requireEditAccess(topic, userDetails);
        List<StudyItemUpsert> payload = catalogService.parseStudyItemsCsv(csv);
        BulkResult result = catalogService.bulkUpsertStudyItems(topicId, payload, dryRun, userDetails != null ? userDetails.getId() : null);
        return ok(result);
    }

    @PutMapping("/{topicId}")
    public ResponseEntity<Topic> updateTopic(
            @PathVariable Long topicId,
            @RequestBody Topic updates,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");
        requireEditAccess(topic, userDetails);
        // Ownership check moved to controller-level helper requiring auth context

        // Manual field updates for clarity
        if (updates.getTitle() != null) {
            topic.setTitle(updates.getTitle());
        }
        if (updates.getDescription() != null) {
            topic.setDescription(updates.getDescription());
        }
        if (updates.getOrderIndex() != null) {
            topic.setOrderIndex(updates.getOrderIndex());
        }

        return ok(catalogService.updateTopic(topic, userDetails != null ? userDetails.getId() : null));
    }

    @DeleteMapping("/{topicId}")
    public ResponseEntity<Void> deleteTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "false") boolean force,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");
        requireEditAccess(topic, userDetails);
        catalogService.deleteTopic(topicId, force, userDetails != null ? userDetails.getId() : null);
        return noContent();
    }

    private void requireEditAccess(Topic topic, UserDetailsImpl userDetails) {
        Long courseId = topic.getCourse() != null ? topic.getCourse().getId() : null;
        Long userId = userDetails != null ? userDetails.getId() : null;
        boolean admin = isAdmin(userDetails);
        if (courseId == null || !catalogService.canEditCourse(courseId, userId, admin)) {
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

    public record BulkItemsRequest(List<StudyItemUpsert> items, Boolean dryRun) {

    }
}
