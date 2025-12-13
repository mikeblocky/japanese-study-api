package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudyItem> updateItem(@PathVariable long id, @RequestBody StudyItem item) {
        return catalogService.getStudyItemById(id)
                .map(existing -> {
                    // Only update the fields that should change, preserve relationships
                    existing.setPrimaryText(item.getPrimaryText());
                    existing.setSecondaryText(item.getSecondaryText());
                    existing.setMeaning(item.getMeaning());
                    // Topic is preserved since we're updating the existing entity
                    return ResponseEntity.ok(catalogService.updateStudyItem(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable long id) {
        catalogService.deleteStudyItem(id);
        return ResponseEntity.noContent().build();
    }
}
