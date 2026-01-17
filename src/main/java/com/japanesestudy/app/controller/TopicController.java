package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<StudyItem>> getTopicItems(
            @PathVariable long id,
            @RequestParam(name = "limit", required = false) Integer limit,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        if (limit != null) {
            return ResponseEntity.ok(catalogService.getItemsByTopic(id, limit));
        }

        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(catalogService.getItemsByTopicForUser(id, userId));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<StudyItem> createItemForTopic(@PathVariable long id, @RequestBody StudyItem item) {
        return catalogService.getTopicById(id)
            .map(topic -> {
                item.setTopic(topic);
                return ResponseEntity.ok(catalogService.createStudyItem(item));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Topic> updateTopic(@PathVariable long id, @RequestBody Topic topic) {
        return catalogService.getTopicById(id)
            .map(existing -> {
                existing.setTitle(topic.getTitle());
                existing.setDescription(topic.getDescription());
                if (topic.getOrderIndex() != null && topic.getOrderIndex() != 0) {
                    existing.setOrderIndex(topic.getOrderIndex());
                }
                return ResponseEntity.ok(catalogService.updateTopic(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable long id) {
        catalogService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}
