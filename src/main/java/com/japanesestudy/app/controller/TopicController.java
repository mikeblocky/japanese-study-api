package com.japanesestudy.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.service.CatalogService;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final CatalogService catalogService;

    public TopicController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<java.util.List<com.japanesestudy.app.entity.StudyItem>> getTopicItems(
            @PathVariable long id,
            @RequestParam(name = "limit", required = false) Integer limit) {
        if (limit != null) {
            return ResponseEntity.ok(catalogService.getItemsByTopic(id, limit));
        }
        return ResponseEntity.ok(catalogService.getItemsByTopic(id));
    }
}
