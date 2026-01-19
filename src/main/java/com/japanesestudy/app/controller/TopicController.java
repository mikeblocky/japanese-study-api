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

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.CatalogService;
import com.japanesestudy.app.service.CatalogService.TopicSummary;
import static com.japanesestudy.app.util.Utils.created;
import static com.japanesestudy.app.util.Utils.getOrThrow;
import static com.japanesestudy.app.util.Utils.noContent;
import static com.japanesestudy.app.util.Utils.ok;
import static com.japanesestudy.app.util.Utils.validateOwnership;

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

    @PostMapping
    public ResponseEntity<Topic> createTopic(@RequestBody Topic topic) {
        return created(catalogService.createTopic(topic));
    }

    @GetMapping("/{topicId}/summary")
    public ResponseEntity<TopicSummary> getTopicSummary(@PathVariable Long topicId) {
        return ok(catalogService.getTopicSummary(topicId));
    }

    @PostMapping("/{topicId}/items")
    public ResponseEntity<StudyItem> addItemToTopic(
            @PathVariable Long topicId,
            @RequestBody StudyItem item,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");
        if (topic.getCourse() != null) {
            Long ownerId = topic.getCourse().getOwner() != null ? topic.getCourse().getOwner().getId() : null;
            validateOwnership(ownerId, userDetails.getId());
        }
        item.setTopic(topic);
        return created(catalogService.createStudyItem(item));
    }

    @PutMapping("/{topicId}")
    public ResponseEntity<Topic> updateTopic(
            @PathVariable Long topicId,
            @RequestBody Topic updates) {
        Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");

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

        return ok(catalogService.updateTopic(topic));
    }

    @DeleteMapping("/{topicId}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long topicId) {
        catalogService.deleteTopic(topicId);
        return noContent();
    }
}
