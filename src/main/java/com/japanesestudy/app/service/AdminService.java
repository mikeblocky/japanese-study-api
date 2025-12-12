package com.japanesestudy.app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.dto.AdminCourseSummary;
import com.japanesestudy.app.dto.AnkiImportRequest;
import com.japanesestudy.app.dto.AnkiItem;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.StudySessionRepository;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final StudySessionRepository sessionRepository;
    private final CacheManager cacheManager;

    public AdminService(
            UserRepository userRepository,
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

    // --- Course Management ---
    @Cacheable(cacheNames = "adminCourseSummaries")
    public List<Map<String, Object>> getAdminCourses() {
        List<AdminCourseSummary> summaries = courseRepository.findAdminCourseSummaries();
        return summaries.stream().map(summary -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", summary.getId());
            map.put("title", summary.getTitle());
            map.put("description", summary.getDescription());
            map.put("level", summary.getLevel());
            map.put("topicCount", summary.getTopicCount());
            map.put("itemCount", summary.getItemCount());
            return map;
        }).collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    @SuppressWarnings("null")
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    @SuppressWarnings("null")
    public Optional<Course> updateCourse(long id, Course updates) {
        return courseRepository.findById(id).map(course -> {
            if (updates.getTitle() != null) {
                course.setTitle(updates.getTitle());
            }
            if (updates.getDescription() != null) {
                course.setDescription(updates.getDescription());
            }
            if (updates.getLevel() != null) {
                course.setLevel(updates.getLevel());
            }
            return courseRepository.save(course);
        });
    }

    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic", "adminCourseSummaries"}, allEntries = true)
    public boolean deleteCourse(long id) {
        if (!courseRepository.existsById(id)) {
            return false;
        }
        courseRepository.deleteById(id);
        return true;
    }

    // --- Anki Import ---
    @Transactional
    @CacheEvict(cacheNames = {"courses", "courseById", "topicsByCourse", "itemsByTopic", "adminCourseSummaries", "adminStats"}, allEntries = true)
    public Map<String, Object> importAnki(AnkiImportRequest request) {
        // Create course
        Course course = new Course();
        course.setTitle(request.getCourseName() != null ? request.getCourseName() : "Imported Course");
        course.setDescription(request.getDescription());
        course.setLevel("Custom");
        course = courseRepository.save(course);

        // Group items by topic - use TreeMap for sorted order
        Map<String, List<AnkiItem>> itemsByTopic = new TreeMap<>();
        for (AnkiItem item : request.getItems()) {
            String topicName = item.getTopic() != null ? item.getTopic() : "Default";
            itemsByTopic.computeIfAbsent(topicName, k -> new ArrayList<>()).add(item);
        }

        // Create topics and study items
        int topicOrder = 0;
        int itemsCreated = 0;
        for (Map.Entry<String, List<AnkiItem>> entry : itemsByTopic.entrySet()) {
            Topic topic = new Topic();
            topic.setTitle(entry.getKey());
            topic.setCourse(course);
            topic.setOrderIndex(topicOrder++);
            topic = topicRepository.save(topic);

            // Batch insert items for this topic
            List<StudyItem> batch = new ArrayList<>(Math.min(entry.getValue().size(), 500));
            for (AnkiItem ankiItem : entry.getValue()) {
                StudyItem studyItem = new StudyItem();
                studyItem.setPrimaryText(ankiItem.getFront() != null ? ankiItem.getFront() : "");
                studyItem.setSecondaryText(ankiItem.getReading() != null ? ankiItem.getReading() : "");
                studyItem.setMeaning(ankiItem.getBack() != null ? ankiItem.getBack() : "");
                studyItem.setTopic(topic);
                studyItem.setType("VOCABULARY");
                batch.add(studyItem);

                if (batch.size() >= 500) {
                    studyItemRepository.saveAll(batch);
                    studyItemRepository.flush();
                    itemsCreated += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                studyItemRepository.saveAll(batch);
                studyItemRepository.flush();
                itemsCreated += batch.size();
            }
        }

        return Map.of(
                "message", "Import successful",
                "courseId", course.getId(),
                "courseName", course.getTitle(),
                "topicsCreated", itemsByTopic.size(),
                "itemsCreated", itemsCreated);
    }

    // --- Statistics ---
    @Cacheable(cacheNames = "adminStats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalCourses", courseRepository.count());
        stats.put("totalItems", studyItemRepository.count());
        stats.put("totalSessions", sessionRepository.count());
        stats.put("javaVersion", System.getProperty("java.version"));
        stats.put("caches", Map.of("users", "active", "courses", "active"));
        return stats;
    }

    public Map<String, Object> getHealth() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = allocatedMemory - freeMemory;

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "CONNECTED");
        health.put("memoryMB", Map.of(
                "used", usedMemory / (1024 * 1024),
                "max", maxMemory / (1024 * 1024)));
        return health;
    }

    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("estimatedStorageKB", 1204); // Placeholder
        return stats;
    }
    // --- User Management ---

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("email", user.getUsername() + "@example.com"); // Placeholder
            map.put("role", user.getRole().name());
            map.put("sessionCount", sessionRepository.countByUserId(user.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    public boolean updateUserRole(long userId, String newRole) {
        if (newRole == null || newRole.isBlank()) {
            return false;
        }
        return userRepository.findById(userId).map(user -> {
            try {
                // Handle "STUDENT" as "USER" if needed, or assume exact match
                String roleName = "STUDENT".equals(newRole) ? "USER" : newRole;
                user.setRole(com.japanesestudy.app.entity.Role.valueOf(roleName));
                userRepository.save(user);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }).orElse(false);
    }

    // --- Cache Management ---
    @SuppressWarnings("null")
    public Map<String, String> clearCache(String cacheName) {
        if (cacheName == null || cacheName.isBlank()) {
            String[] cacheNames = {
                "courses",
                "courseById",
                "topicsByCourse",
                "itemsByTopic",
                "adminCourseSummaries",
                "adminStats"
            };
            for (String name : cacheNames) {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
            return Map.of("message", "All caches cleared");
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return Map.of("message", "Cache not found: " + cacheName);
        }
        cache.clear();
        return Map.of("message", "Cache '" + cacheName + "' cleared");
    }
}
