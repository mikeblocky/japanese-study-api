package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final CatalogService catalogService;

    @GetMapping("/{id}")
    public ResponseEntity<Topic> getTopicById(@PathVariable long id) {
        return catalogService.getTopicById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<java.util.List<StudyItem>> getTopicItems(
            @PathVariable long id,
            @RequestParam(name = "limit", required = false) Integer limit) {
        if (limit != null) {
            return ResponseEntity.ok(catalogService.getItemsByTopic(id, limit));
        }
        return ResponseEntity.ok(catalogService.getItemsByTopic(id));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudyItem> createItemForTopic(@PathVariable long id, @RequestBody StudyItem item) {
        return catalogService.getTopicById(id)
                .map(topic -> {
                    item.setTopic(topic);
                    StudyItem created = catalogService.createStudyItem(item);
                    return ResponseEntity.ok(created);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Topic> updateTopic(@PathVariable long id, @RequestBody Topic topic) {
        return catalogService.getTopicById(id)
                .map(existing -> {
                    // Only update the fields that should change, preserve relationships
                    existing.setTitle(topic.getTitle());
                    existing.setDescription(topic.getDescription());
                    if (topic.getOrderIndex() != 0) {
                        existing.setOrderIndex(topic.getOrderIndex());
                    }
                    // Course and items are preserved since we're updating the existing entity
                    return ResponseEntity.ok(catalogService.updateTopic(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTopic(@PathVariable long id) {
        catalogService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}
