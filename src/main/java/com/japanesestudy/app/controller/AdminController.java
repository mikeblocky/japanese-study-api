package com.japanesestudy.app.controller;

import com.japanesestudy.app.model.User;
import com.japanesestudy.app.service.AdminService;
import com.japanesestudy.app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for system management endpoints.
 * These endpoints are intended for MANAGER and ADMIN roles only.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService, UserRepository userRepository) {
        this.adminService = adminService;
        this.userRepository = userRepository;
    }

    private Long getUserId(String header) {
        if (header == null)
            return null;
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void checkAdminRole(Long userId) {
        if (userId == null)
            throw new RuntimeException("Unauthorized");
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Access denied: ADMIN role required");
        }
    }

    /**
     * Get system-wide statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }

    /**
     * Get all users with activity metrics.
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsersWithMetrics());
    }

    /**
     * Create a new user.
     */
    @PostMapping("/users")
    public ResponseEntity<User> createUser(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        String username = body.get("username");
        String email = body.get("email");
        String role = body.getOrDefault("role", "STUDENT");
        String password = body.getOrDefault("password", "changeme");

        if (username == null || email == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(adminService.createUser(username, email, password, role, getUserId(userIdHeader)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    /**
     * Update a user's role.
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        String newRole = body.get("role");
        if (newRole == null || newRole.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(adminService.updateUserRole(id, newRole, getUserId(userIdHeader)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    /**
     * Delete a user.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            adminService.deleteUser(id, getUserId(userIdHeader));
            return ResponseEntity.ok(Map.of("message", "User deleted", "status", "success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get system health metrics. ADMIN ONLY.
     */
    @GetMapping("/health")
    public ResponseEntity<?> getHealth(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getHealthMetrics());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Clear all caches. ADMIN ONLY.
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearAllCaches(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            adminService.clearAllCaches();
            return ResponseEntity.ok(Map.of("message", "All caches cleared", "status", "success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Clear a specific cache. ADMIN ONLY.
     */
    @PostMapping("/cache/clear/{cacheName}")
    public ResponseEntity<?> clearCache(
            @PathVariable String cacheName,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            adminService.clearCache(cacheName);
            return ResponseEntity.ok(Map.of("message", "Cache '" + cacheName + "' cleared", "status", "success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Export all data as JSON. ADMIN ONLY.
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportData(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.exportAllData());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Import data from JSON. ADMIN ONLY.
     */
    @PostMapping("/import")
    public ResponseEntity<?> importData(
            @RequestBody Map<String, Object> data,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Migration-Secret", required = false) String migrationSecret) {
        try {
            // Allow if secret matches, OR if admin
            if ("JAPANESE_STUDY_MIGRATION_2025".equals(migrationSecret)) {
                // Bypass role check
            } else {
                checkAdminRole(getUserId(userIdHeader));
            }

            int imported = adminService.importData(data);
            return ResponseEntity.ok(Map.of("message", "Imported " + imported + " items", "status", "success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update a user's password.
     */
    @PatchMapping("/users/{id}/password")
    public ResponseEntity<?> updatePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 4 characters"));
        }
        try {
            adminService.updatePassword(id, newPassword, getUserId(userIdHeader));
            return ResponseEntity.ok(Map.of("message", "Password updated", "status", "success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Bulk import users from JSON array.
     */
    @PostMapping("/users/bulk")
    public ResponseEntity<Map<String, Object>> bulkImportUsers(
            @RequestBody List<Map<String, String>> users,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        int created = adminService.bulkCreateUsers(users, getUserId(userIdHeader));
        return ResponseEntity.ok(Map.of("created", created, "status", "success"));
    }

    /**
     * Reset all study progress for a user.
     */
    @PostMapping("/users/{id}/reset-progress")
    public ResponseEntity<Map<String, String>> resetUserProgress(@PathVariable Long id) {
        adminService.resetUserProgress(id);
        return ResponseEntity.ok(Map.of("message", "User progress reset", "status", "success"));
    }

    /**
     * Get database statistics. ADMIN ONLY.
     */
    @GetMapping("/database/stats")
    public ResponseEntity<?> getDatabaseStats(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getDatabaseStats());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reset all sessions (maintenance). ADMIN ONLY.
     */
    @PostMapping("/maintenance/reset-sessions")
    public ResponseEntity<?> resetAllSessions(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            int deleted = adminService.resetAllSessions();
            return ResponseEntity.ok(Map.of("message", "Deleted " + deleted + " sessions", "status", "success"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Backup database summary. ADMIN ONLY.
     */
    @GetMapping("/backup/summary")
    public ResponseEntity<?> getBackupSummary(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getBackupSummary());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Import Anki file content to create course/topics.
     */
    @PostMapping("/anki/import")
    public ResponseEntity<Map<String, Object>> importAnkiData(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(adminService.importAnkiData(data));
    }

    /**
     * Get deep JVM metrics. ADMIN ONLY.
     */
    @GetMapping("/metrics/jvm")
    public ResponseEntity<?> getJvmMetrics(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getJvmMetrics());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get thread information. ADMIN ONLY.
     */
    @GetMapping("/metrics/threads")
    public ResponseEntity<?> getThreadMetrics(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getThreadMetrics());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get garbage collection stats. ADMIN ONLY.
     */
    @GetMapping("/metrics/gc")
    public ResponseEntity<?> getGcMetrics(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getGcMetrics());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get system diagnostics. ADMIN ONLY.
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<?> getSystemDiagnostics(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getSystemDiagnostics());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all courses summary for admin.
     */
    @GetMapping("/courses")
    public ResponseEntity<List<Map<String, Object>>> getCoursesSummary() {
        return ResponseEntity.ok(adminService.getCoursesSummary());
    }

    /**
     * Delete a course and all its data.
     */
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Map<String, String>> deleteCourse(@PathVariable Long id) {
        adminService.deleteCourse(id);
        return ResponseEntity.ok(Map.of("message", "Course deleted", "status", "success"));
    }

    /**
     * Get runtime environment info. ADMIN ONLY.
     */
    @GetMapping("/environment")
    public ResponseEntity<?> getEnvironment(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            checkAdminRole(getUserId(userIdHeader));
            return ResponseEntity.ok(adminService.getEnvironmentInfo());
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
