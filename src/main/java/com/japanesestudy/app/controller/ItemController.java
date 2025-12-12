package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.repository.StudyItemRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final StudyItemRepository studyItemRepository;

    public ItemController(StudyItemRepository studyItemRepository) {
        this.studyItemRepository = studyItemRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public StudyItem createItem(@Valid @NonNull @RequestBody StudyItem item) {
        return studyItemRepository.save(item);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public List<StudyItem> createItems(@NonNull @RequestBody List<StudyItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        return studyItemRepository.saveAll(items);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public ResponseEntity<StudyItem> updateItem(@PathVariable long id, @Valid @NonNull @RequestBody StudyItem itemDetails) {
        return studyItemRepository.findById(id).map(item -> {
            item.setPrimaryText(itemDetails.getPrimaryText());
            item.setSecondaryText(itemDetails.getSecondaryText());
            item.setMeaning(itemDetails.getMeaning());
            item.setImageUrl(itemDetails.getImageUrl());
            item.setAudioUrl(itemDetails.getAudioUrl());
            // Update other fields as necessary
            return ResponseEntity.ok(studyItemRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = {"itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public ResponseEntity<?> deleteItem(@PathVariable long id) {
        if (!studyItemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        studyItemRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
