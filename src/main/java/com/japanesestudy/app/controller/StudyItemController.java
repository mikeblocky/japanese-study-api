package com.japanesestudy.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.service.CatalogService;

@RestController
@RequestMapping("/api/items")
public class StudyItemController {

    private final CatalogService catalogService;

    public StudyItemController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

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
                    item.setId(id);
                    item.setTopic(existing.getTopic());
                    return ResponseEntity.ok(catalogService.updateStudyItem(item));
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
