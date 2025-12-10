package com.japanesestudy.app.service;

import com.japanesestudy.app.model.User;
import com.japanesestudy.app.repository.*;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for admin operations including system stats and user management.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final StudySessionRepository sessionRepository;
    private final CacheManager cacheManager;

    public AdminService(UserRepository userRepository,
            CourseRepository courseRepository,
            TopicRepository topicRepository,
            StudyItemRepository studyItemRepository,
            StudySessionRepository sessionRepository,
            CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.studyItemRepository = studyItemRepository;
        this.sessionRepository = sessionRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Get system-wide statistics for admin dashboard.
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Entity counts
        stats.put("totalUsers", userRepository.count());
        stats.put("totalCourses", courseRepository.count());
        stats.put("totalTopics", topicRepository.count());
        stats.put("totalItems", studyItemRepository.count());
        stats.put("totalSessions", sessionRepository.count());

        // Cache info
        Collection<String> cacheNames = cacheManager.getCacheNames();
        Map<String, String> cacheStatus = new LinkedHashMap<>();
        for (String name : cacheNames) {
            cacheStatus.put(name, "ACTIVE");
        }
        stats.put("caches", cacheStatus);

        // System info
        stats.put("javaVersion", System.getProperty("java.version"));
        stats.put("maxMemoryMB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        stats.put("usedMemoryMB",
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
        stats.put("serverTime", LocalDateTime.now().toString());

        return stats;
    }

    /**
     * Get all users with activity metrics for admin management.
     */
    public List<Map<String, Object>> getAllUsersWithMetrics() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole());

            // Session count for this user
            long sessionCount = sessionRepository.findByUserId(user.getId()).size();
            userData.put("sessionCount", sessionCount);

            result.add(userData);
        }

        return result;
    }

    /**
     * Update a user's role.
     */
    public User updateUserRole(Long userId, String newRole, Long requestingUserId) {
        checkAdminAccess(requestingUserId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Validate role
        if (!List.of("STUDENT", "MANAGER", "ADMIN").contains(newRole.toUpperCase())) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }

        user.setRole(newRole.toUpperCase());
        return userRepository.save(user);
    }

    /**
     * Clear all caches.
     */
    public void clearAllCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    /**
     * Clear a specific cache by name.
     */
    public void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            throw new RuntimeException("Cache not found: " + cacheName);
        }
    }

    /**
     * Get system health metrics.
     */
    public Map<String, Object> getHealthMetrics() {
        Map<String, Object> health = new LinkedHashMap<>();

        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());

        // Memory
        Runtime runtime = Runtime.getRuntime();
        Map<String, Long> memory = new LinkedHashMap<>();
        memory.put("total", runtime.totalMemory() / (1024 * 1024));
        memory.put("free", runtime.freeMemory() / (1024 * 1024));
        memory.put("used", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("max", runtime.maxMemory() / (1024 * 1024));
        health.put("memoryMB", memory);

        // Database connection test
        try {
            userRepository.count();
            health.put("database", "CONNECTED");
        } catch (Exception e) {
            health.put("database", "ERROR: " + e.getMessage());
        }

        return health;
    }

    /**
     * Create a new user.
     */
    public User createUser(String username, String email, String password, String role, Long requestingUserId) {
        // Prevent MANAGER from creating ADMIN
        if (requestingUserId != null && "ADMIN".equalsIgnoreCase(role)) {
            User requester = userRepository.findById(requestingUserId).orElse(null);
            if (requester != null && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
                throw new RuntimeException("Insufficient permissions to create ADMIN user");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role.toUpperCase());
        return userRepository.save(user);
    }

    /**
     * Delete a user.
     */
    public void deleteUser(Long userId, Long requestingUserId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found: " + userId);
        }
        checkAdminAccess(requestingUserId, userId);
        userRepository.deleteById(userId);
    }

    /**
     * Export all data for backup.
     */
    public Map<String, Object> exportAllData() {
        Map<String, Object> data = new LinkedHashMap<>();

        // Users (without passwords)
        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id", user.getId());
            u.put("username", user.getUsername());
            u.put("email", user.getEmail());
            u.put("role", user.getRole());
            users.add(u);
        }
        data.put("users", users);

        // Stats
        data.put("totalCourses", courseRepository.count());
        data.put("totalTopics", topicRepository.count());
        data.put("totalItems", studyItemRepository.count());
        data.put("totalSessions", sessionRepository.count());
        data.put("exportedAt", LocalDateTime.now().toString());

        return data;
    }

    /**
     * Import data from backup.
     */
    @SuppressWarnings("unchecked")
    public int importData(Map<String, Object> data) {
        int importCount = 0;

        // Import users if present
        if (data.containsKey("users")) {
            List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("users");
            for (Map<String, Object> userData : users) {
                String username = (String) userData.get("username");
                String email = (String) userData.get("email");
                String role = (String) userData.getOrDefault("role", "STUDENT");

                // Only create if not exists
                if (userRepository.findAll().stream().noneMatch(u -> u.getUsername().equals(username))) {
                    createUser(username, email, "changeme", role, null);
                    importCount++;
                }
            }
        }

        // Import Courses / Topics / Items
        if (data.containsKey("courses")) {
            List<Map<String, Object>> courses = (List<Map<String, Object>>) data.get("courses");
            for (Map<String, Object> courseData : courses) {
                // 1. Create/Find Course
                String courseTitle = (String) courseData.get("title");
                var existingCourse = courseRepository.findAll().stream()
                        .filter(c -> c.getTitle().equals(courseTitle))
                        .findFirst();

                com.japanesestudy.app.model.Course course;
                if (existingCourse.isPresent()) {
                    course = existingCourse.get();
                } else {
                    course = new com.japanesestudy.app.model.Course();
                    course.setTitle(courseTitle);
                    course.setDescription((String) courseData.get("description"));
                    course = courseRepository.save(course);
                    importCount++;
                }

                // 2. Topics
                if (courseData.containsKey("topics")) {
                    List<Map<String, Object>> topics = (List<Map<String, Object>>) courseData.get("topics");
                    for (Map<String, Object> topicData : topics) {
                        String topicTitle = (String) topicData.get("title");

                        // Check if topic exists in this course
                        var finalCourseId = course.getId();
                        var existingTopic = topicRepository.findByCourseId(finalCourseId).stream()
                                .filter(t -> t.getTitle().equals(topicTitle))
                                .findFirst();

                        com.japanesestudy.app.model.Topic topic;
                        if (existingTopic.isPresent()) {
                            topic = existingTopic.get();
                        } else {
                            topic = new com.japanesestudy.app.model.Topic();
                            topic.setTitle(topicTitle);
                            topic.setDescription((String) topicData.get("description"));
                            topic.setCourse(course);
                            topic = topicRepository.save(topic);
                            importCount++;
                        }

                        // 3. Items
                        if (topicData.containsKey("items")) {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) topicData.get("items");
                            for (Map<String, Object> itemData : items) {
                                com.japanesestudy.app.model.StudyItem item = new com.japanesestudy.app.model.StudyItem();

                                // Handle mismatched field names (JSON vs Java)
                                String primary = (String) itemData.getOrDefault("primaryText",
                                        itemData.getOrDefault("word", ""));
                                String secondary = (String) itemData.getOrDefault("secondaryText",
                                        itemData.getOrDefault("reading", ""));
                                String meaning = (String) itemData.getOrDefault("meaning", "");
                                String detailed = (String) itemData.getOrDefault("detailedInfo",
                                        itemData.getOrDefault("exampleSentence", ""));

                                item.setPrimaryText(primary);
                                item.setSecondaryText(secondary);
                                item.setMeaning(meaning);
                                item.setDetailedInfo(detailed);
                                item.setAudioUrl((String) itemData.getOrDefault("audioUrl", ""));
                                item.setTopic(topic);

                                studyItemRepository.save(item);
                                importCount++;
                            }
                        }
                    }
                }
            }
        }

        return importCount;
    }

    /**
     * Update a user's password.
     */
    public void updatePassword(Long userId, String newPassword, Long requestingUserId) {
        checkAdminAccess(requestingUserId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    /**
     * Bulk create users from a list.
     */
    public int bulkCreateUsers(List<Map<String, String>> usersData, Long requestingUserId) {
        int created = 0;
        for (Map<String, String> userData : usersData) {
            String username = userData.get("username");
            String email = userData.get("email");
            String role = userData.getOrDefault("role", "STUDENT");
            String password = userData.getOrDefault("password", "changeme");

            // Only create if username doesn't exist
            if (username != null && userRepository.findAll().stream()
                    .noneMatch(u -> u.getUsername().equals(username))) {
                createUser(username, email, password, role, requestingUserId);
                created++;
            }
        }
        return created;
    }

    /**
     * Helper to verify if requester has permission to modify target user.
     * Throws exception if MANAGER tries to modify ADMIN.
     */
    private void checkAdminAccess(Long requestingUserId, Long targetUserId) {
        if (requestingUserId == null)
            return; // Should not happen in prod ideally
        User requester = userRepository.findById(requestingUserId).orElse(null);
        User target = userRepository.findById(targetUserId).orElse(null);

        if (requester != null && target != null) {
            // If requester is not ADMIN (e.g. MANAGER)
            if (!"ADMIN".equalsIgnoreCase(requester.getRole())) {
                // Cannot modify ADMIN
                if ("ADMIN".equalsIgnoreCase(target.getRole())) {
                    throw new RuntimeException("Insufficient permissions to modify ADMIN user");
                }
            }
        }
    }

    /**
     * Reset all study progress for a user.
     */
    public void resetUserProgress(Long userId) {
        // Delete all sessions for this user
        List<com.japanesestudy.app.model.StudySession> sessions = sessionRepository.findByUserId(userId);
        sessionRepository.deleteAll(sessions);
    }

    /**
     * Get database statistics.
     */
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("users", userRepository.count());
        stats.put("courses", courseRepository.count());
        stats.put("topics", topicRepository.count());
        stats.put("items", studyItemRepository.count());
        stats.put("sessions", sessionRepository.count());

        // Calculate storage estimate (rough)
        long estimatedBytes = (userRepository.count() * 200) +
                (courseRepository.count() * 500) +
                (topicRepository.count() * 300) +
                (studyItemRepository.count() * 400) +
                (sessionRepository.count() * 100);
        stats.put("estimatedStorageKB", estimatedBytes / 1024);

        stats.put("timestamp", LocalDateTime.now().toString());

        return stats;
    }

    /**
     * Reset all sessions (maintenance operation).
     */
    public int resetAllSessions() {
        int count = (int) sessionRepository.count();
        sessionRepository.deleteAll();
        return count;
    }

    /**
     * Get backup summary.
     */
    public Map<String, Object> getBackupSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("totalUsers", userRepository.count());
        summary.put("totalCourses", courseRepository.count());
        summary.put("totalTopics", topicRepository.count());
        summary.put("totalItems", studyItemRepository.count());
        summary.put("totalSessions", sessionRepository.count());

        // Last backup info (mock for now)
        summary.put("lastBackup", "Never");
        summary.put("backupReady", true);
        summary.put("generatedAt", LocalDateTime.now().toString());

        return summary;
    }

    /**
     * Import Anki data to create a new course with topics.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> importAnkiData(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        int itemsCreated = 0;
        int topicsCreated = 0;

        String courseName = (String) data.getOrDefault("courseName", "Imported Course");
        String courseDescription = (String) data.getOrDefault("description", "Imported from Anki");

        // Create course
        com.japanesestudy.app.model.Course course = new com.japanesestudy.app.model.Course();
        course.setTitle(courseName);
        course.setDescription(courseDescription);
        course = courseRepository.save(course);

        // Import items by topic
        if (data.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            Map<String, com.japanesestudy.app.model.Topic> topicMap = new LinkedHashMap<>();

            for (Map<String, Object> item : items) {
                String topicName = (String) item.getOrDefault("topic", "Default");

                // Get or create topic
                com.japanesestudy.app.model.Topic topic = topicMap.get(topicName);
                if (topic == null) {
                    topic = new com.japanesestudy.app.model.Topic();
                    topic.setTitle(topicName);
                    topic.setCourse(course);
                    topic = topicRepository.save(topic);
                    topicMap.put(topicName, topic);
                    topicsCreated++;
                }

                // Create study item
                com.japanesestudy.app.model.StudyItem studyItem = new com.japanesestudy.app.model.StudyItem();
                studyItem.setPrimaryText((String) item.getOrDefault("front", ""));
                studyItem.setSecondaryText((String) item.getOrDefault("reading", ""));
                studyItem.setMeaning((String) item.getOrDefault("back", ""));
                studyItem.setTopic(topic);
                studyItemRepository.save(studyItem);
                itemsCreated++;
            }
        }

        result.put("courseId", course.getId());
        result.put("courseName", courseName);
        result.put("topicsCreated", topicsCreated);
        result.put("itemsCreated", itemsCreated);
        result.put("status", "success");

        return result;
    }

    /**
     * Get deep JVM metrics.
     */
    public Map<String, Object> getJvmMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();

        // Memory
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("heapMax", runtime.maxMemory() / (1024 * 1024));
        memory.put("heapUsed", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("heapFree", runtime.freeMemory() / (1024 * 1024));
        memory.put("heapTotal", runtime.totalMemory() / (1024 * 1024));
        metrics.put("memory", memory);

        // System properties
        Map<String, String> jvm = new LinkedHashMap<>();
        jvm.put("version", System.getProperty("java.version"));
        jvm.put("vendor", System.getProperty("java.vendor"));
        jvm.put("vmName", System.getProperty("java.vm.name"));
        jvm.put("vmVersion", System.getProperty("java.vm.version"));
        jvm.put("osName", System.getProperty("os.name"));
        jvm.put("osVersion", System.getProperty("os.version"));
        jvm.put("osArch", System.getProperty("os.arch"));
        metrics.put("jvm", jvm);

        // Processors
        metrics.put("availableProcessors", runtime.availableProcessors());
        metrics.put("timestamp", LocalDateTime.now().toString());

        return metrics;
    }

    /**
     * Get thread information.
     */
    public Map<String, Object> getThreadMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup().getParent();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }

        metrics.put("activeThreadCount", Thread.activeCount());
        metrics.put("peakThreadCount", rootGroup.activeCount());

        // Thread states
        Map<Thread.State, Integer> stateCounts = new LinkedHashMap<>();
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int count = Thread.enumerate(threads);

        for (int i = 0; i < count; i++) {
            Thread t = threads[i];
            if (t != null) {
                stateCounts.merge(t.getState(), 1, Integer::sum);
            }
        }
        metrics.put("threadStates", stateCounts);

        // Sample thread names
        List<String> sampleThreads = new ArrayList<>();
        for (int i = 0; i < Math.min(10, count); i++) {
            if (threads[i] != null) {
                sampleThreads.add(threads[i].getName() + " [" + threads[i].getState() + "]");
            }
        }
        metrics.put("sampleThreads", sampleThreads);
        metrics.put("timestamp", LocalDateTime.now().toString());

        return metrics;
    }

    /**
     * Get garbage collection statistics.
     */
    public Map<String, Object> getGcMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        List<java.lang.management.GarbageCollectorMXBean> gcBeans = java.lang.management.ManagementFactory
                .getGarbageCollectorMXBeans();

        List<Map<String, Object>> collectors = new ArrayList<>();
        long totalGcCount = 0;
        long totalGcTime = 0;

        for (java.lang.management.GarbageCollectorMXBean gc : gcBeans) {
            Map<String, Object> gcInfo = new LinkedHashMap<>();
            gcInfo.put("name", gc.getName());
            gcInfo.put("collectionCount", gc.getCollectionCount());
            gcInfo.put("collectionTimeMs", gc.getCollectionTime());
            collectors.add(gcInfo);

            totalGcCount += gc.getCollectionCount();
            totalGcTime += gc.getCollectionTime();
        }

        metrics.put("collectors", collectors);
        metrics.put("totalCollections", totalGcCount);
        metrics.put("totalGcTimeMs", totalGcTime);
        metrics.put("timestamp", LocalDateTime.now().toString());

        return metrics;
    }

    /**
     * Get comprehensive system diagnostics.
     */
    public Map<String, Object> getSystemDiagnostics() {
        Map<String, Object> diag = new LinkedHashMap<>();

        // Uptime
        java.lang.management.RuntimeMXBean runtimeMXBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeMXBean.getUptime();
        diag.put("uptimeMs", uptimeMs);
        diag.put("uptimeFormatted", formatUptime(uptimeMs));
        diag.put("startTime", new java.util.Date(runtimeMXBean.getStartTime()).toString());

        // Class loading
        java.lang.management.ClassLoadingMXBean classBean = java.lang.management.ManagementFactory
                .getClassLoadingMXBean();
        Map<String, Object> classes = new LinkedHashMap<>();
        classes.put("loaded", classBean.getLoadedClassCount());
        classes.put("totalLoaded", classBean.getTotalLoadedClassCount());
        classes.put("unloaded", classBean.getUnloadedClassCount());
        diag.put("classes", classes);

        // Compilation
        java.lang.management.CompilationMXBean compBean = java.lang.management.ManagementFactory.getCompilationMXBean();
        if (compBean != null) {
            Map<String, Object> compilation = new LinkedHashMap<>();
            compilation.put("name", compBean.getName());
            if (compBean.isCompilationTimeMonitoringSupported()) {
                compilation.put("totalCompilationTimeMs", compBean.getTotalCompilationTime());
            }
            diag.put("compilation", compilation);
        }

        diag.put("timestamp", LocalDateTime.now().toString());
        return diag;
    }

    private String formatUptime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        return String.format("%dd %dh %dm %ds", days, hours % 24, minutes % 60, seconds % 60);
    }

    /**
     * Get courses summary for admin.
     */
    public List<Map<String, Object>> getCoursesSummary() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (com.japanesestudy.app.model.Course course : courseRepository.findAll()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", course.getId());
            c.put("title", course.getTitle());
            c.put("description", course.getDescription());

            List<com.japanesestudy.app.model.Topic> topics = topicRepository.findByCourseId(course.getId());
            c.put("topicCount", topics.size());

            int itemCount = 0;
            for (com.japanesestudy.app.model.Topic t : topics) {
                itemCount += studyItemRepository.findByTopicId(t.getId()).size();
            }
            c.put("itemCount", itemCount);

            result.add(c);
        }

        return result;
    }

    /**
     * Delete a course and all its data.
     */
    public void deleteCourse(Long courseId) {
        com.japanesestudy.app.model.Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // Delete all items in all topics
        List<com.japanesestudy.app.model.Topic> topics = topicRepository.findByCourseId(courseId);
        for (com.japanesestudy.app.model.Topic topic : topics) {
            studyItemRepository.deleteAll(studyItemRepository.findByTopicId(topic.getId()));
        }

        // Delete topics
        topicRepository.deleteAll(topics);

        // Delete course
        courseRepository.delete(course);
    }

    /**
     * Get environment information.
     */
    public Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> env = new LinkedHashMap<>();

        // System
        env.put("osName", System.getProperty("os.name"));
        env.put("osVersion", System.getProperty("os.version"));
        env.put("osArch", System.getProperty("os.arch"));
        env.put("userDir", System.getProperty("user.dir"));
        env.put("userHome", System.getProperty("user.home"));
        env.put("javaHome", System.getProperty("java.home"));
        env.put("tempDir", System.getProperty("java.io.tmpdir"));

        // Network
        try {
            env.put("hostname", java.net.InetAddress.getLocalHost().getHostName());
            env.put("hostAddress", java.net.InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            env.put("hostname", "unknown");
        }

        // File system
        java.io.File[] roots = java.io.File.listRoots();
        List<Map<String, Object>> disks = new ArrayList<>();
        for (java.io.File root : roots) {
            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("path", root.getAbsolutePath());
            disk.put("totalSpaceGB", root.getTotalSpace() / (1024 * 1024 * 1024));
            disk.put("freeSpaceGB", root.getFreeSpace() / (1024 * 1024 * 1024));
            disk.put("usableSpaceGB", root.getUsableSpace() / (1024 * 1024 * 1024));
            disks.add(disk);
        }
        env.put("disks", disks);

        env.put("timestamp", LocalDateTime.now().toString());
        return env;
    }
}
