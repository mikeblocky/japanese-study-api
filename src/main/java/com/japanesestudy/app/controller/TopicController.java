package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    @Autowired
    TopicRepository topicRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Topic createTopic(@RequestBody Topic topic) {
        return topicRepository.save(topic);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.japanesestudy.app.entity.StudyItem>> getTopicItems(@PathVariable Long id) {
        return topicRepository.findById(id)
                .map(topic -> ResponseEntity.ok(topic.getStudyItems()))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTopic(@PathVariable Long id) {
        if (!topicRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        topicRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
