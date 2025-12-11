package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // --- Course Management ---

    @GetMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getAdminCourses() {
        return adminService.getAdminCourses();
    }

    @PostMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public Course createCourse(@RequestBody Course course) {
        return adminService.createCourse(course);
    }

    @PatchMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course updates) {
        return adminService.updateCourse(id, updates)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        if (adminService.deleteCourse(id)) {
            return ResponseEntity.ok(Map.of("message", "Course deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    // --- Anki Import ---

    @PostMapping("/anki/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importAnki(@Valid @RequestBody AnkiImportRequest request) {
        try {
            Map<String, Object> result = adminService.importAnki(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Import failed: " + e.getMessage()));
        }
    }

    // --- Statistics ---

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getHealth() {
        return ResponseEntity.ok(adminService.getHealth());
    }

    @GetMapping("/database/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDatabaseStats() {
        return ResponseEntity.ok(adminService.getDatabaseStats());
    }
}
