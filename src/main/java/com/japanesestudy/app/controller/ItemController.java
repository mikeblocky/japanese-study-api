package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.repository.StudyItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    StudyItemRepository studyItemRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StudyItem createItem(@RequestBody StudyItem item) {
        return studyItemRepository.save(item);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudyItem> createItems(@RequestBody List<StudyItem> items) {
        return studyItemRepository.saveAll(items);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudyItem> updateItem(@PathVariable Long id, @RequestBody StudyItem itemDetails) {
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
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        if (!studyItemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        studyItemRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
