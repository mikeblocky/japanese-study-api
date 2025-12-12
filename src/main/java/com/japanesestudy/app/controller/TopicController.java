package com.japanesestudy.app.controller;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.service.CatalogService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicRepository topicRepository;
    private final CatalogService catalogService;

    public TopicController(TopicRepository topicRepository, CatalogService catalogService) {
        this.topicRepository = topicRepository;
        this.catalogService = catalogService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"topicsByCourse", "itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public Topic createTopic(@Valid @NonNull @RequestBody Topic topic) {
        return topicRepository.save(topic);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.japanesestudy.app.entity.StudyItem>> getTopicItems(
            @PathVariable long id,
            @RequestParam(name = "limit", required = false) Integer limit) {
        if (limit != null) {
            return ResponseEntity.ok(catalogService.getItemsByTopic(id, limit));
        }
        return ResponseEntity.ok(catalogService.getItemsByTopic(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"topicsByCourse", "itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public ResponseEntity<?> deleteTopic(@PathVariable long id) {
        if (!topicRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        topicRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
