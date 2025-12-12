package com.japanesestudy.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.japanesestudy.app.service.CatalogService;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final CatalogService catalogService;

    public TopicController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

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
                    topic.setId(id);
                    topic.setCourse(existing.getCourse());
                    return ResponseEntity.ok(catalogService.updateTopic(topic));
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
