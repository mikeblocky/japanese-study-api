package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.japanesestudy.app.util.Utils.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class StudyItemController {

    private final CatalogService catalogService;

    @GetMapping("/{itemId}")
    public ResponseEntity<StudyItem> getItemById(@PathVariable Long itemId) {
        return catalogService.getStudyItemById(itemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<StudyItem> updateItem(
            @PathVariable Long itemId,
            @RequestBody StudyItem updates) {
        StudyItem item = getOrThrow(() -> catalogService.getStudyItemById(itemId), "Study item not found");
        
        // Manual field updates for clarity
        if (updates.getPrimaryText() != null) item.setPrimaryText(updates.getPrimaryText());
        if (updates.getSecondaryText() != null) item.setSecondaryText(updates.getSecondaryText());
        if (updates.getMeaning() != null) item.setMeaning(updates.getMeaning());
        if (updates.getAdditionalData() != null) {
            item.setAdditionalData(new java.util.HashMap<>(updates.getAdditionalData()));
        }
        
        return ok(catalogService.updateStudyItem(item));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        catalogService.deleteStudyItem(itemId);
        return noContent();
    }
}
