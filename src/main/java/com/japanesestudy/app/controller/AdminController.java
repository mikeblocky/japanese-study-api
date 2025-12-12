package com.japanesestudy.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.service.AdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // --- Course Management ---
    @GetMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getAdminCourses() {
        return adminService.getAdminCourses();
    }

    @PostMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public Course createCourse(@Valid @NonNull @RequestBody Course course) {
        return adminService.createCourse(course);
    }

    @PatchMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable long id, @Valid @NonNull @RequestBody Course updates) {
        return adminService.updateCourse(id, updates)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable long id) {
        if (adminService.deleteCourse(id)) {
            return ResponseEntity.ok(Map.of("message", "Course deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    // --- Anki Import ---
    @PostMapping("/anki/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importAnki(@Valid @NonNull @RequestBody AnkiImportRequest request) {
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
    // --- User Management ---

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getUsers() {
        return adminService.getAllUsers();
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable long id, @NonNull @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (adminService.updateUserRole(id, role)) {
            return ResponseEntity.ok(Map.of("message", "Role updated"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Failed to update role"));
    }

    // --- Cache Management ---
    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearAllCaches() {
        return ResponseEntity.ok(adminService.clearCache(null));
    }

    @PostMapping("/cache/clear/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearCache(@PathVariable String name) {
        return ResponseEntity.ok(adminService.clearCache(name));
    }
}
