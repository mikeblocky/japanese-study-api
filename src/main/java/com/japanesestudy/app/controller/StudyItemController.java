package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class StudyItemController {

    private final CatalogService catalogService;

    @GetMapping("/{id}")
    public ResponseEntity<StudyItem> getItemById(@PathVariable long id) {
        return catalogService.getStudyItemById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudyItem> updateItem(@PathVariable long id, @RequestBody StudyItem item) {
        return catalogService.getStudyItemById(id)
            .map(existing -> {
                existing.setPrimaryText(item.getPrimaryText());
                existing.setSecondaryText(item.getSecondaryText());
                existing.setMeaning(item.getMeaning());
                // Create a NEW HashMap to ensure JPA detects the change
                Map<String, String> newData = new java.util.HashMap<>();
                if (item.getAdditionalData() != null) {
                    newData.putAll(item.getAdditionalData());
                }
                existing.setAdditionalData(newData);
                return ResponseEntity.ok(catalogService.updateStudyItem(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable long id) {
        catalogService.deleteStudyItem(id);
        return ResponseEntity.noContent().build();
    }
}
