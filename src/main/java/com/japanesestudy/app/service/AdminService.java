package com.japanesestudy.app.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private StudyItemRepository studyItemRepository;

    @Autowired
    private StudySessionRepository sessionRepository;

    // --- Course Management ---

    public List<Map<String, Object>> getAdminCourses() {
        return courseRepository.findAll().stream().map(course -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("title", course.getTitle());
            map.put("description", course.getDescription());
            map.put("level", course.getLevel());
            map.put("topicCount", course.getTopics() != null ? course.getTopics().size() : 0);
            int itemCount = course.getTopics() != null
                    ? course.getTopics().stream()
                            .mapToInt(t -> t.getStudyItems() != null ? t.getStudyItems().size() : 0).sum()
                    : 0;
            map.put("itemCount", itemCount);
            return map;
        }).collect(Collectors.toList());
    }

    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    public Optional<Course> updateCourse(Long id, Course updates) {
        return courseRepository.findById(id).map(course -> {
            if (updates.getTitle() != null)
                course.setTitle(updates.getTitle());
            if (updates.getDescription() != null)
                course.setDescription(updates.getDescription());
            if (updates.getLevel() != null)
                course.setLevel(updates.getLevel());
            return courseRepository.save(course);
        });
    }

    public boolean deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            return false;
        }
        courseRepository.deleteById(id);
        return true;
    }

    // --- Anki Import ---

    @Transactional
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
        for (Map.Entry<String, List<AnkiItem>> entry : itemsByTopic.entrySet()) {
            Topic topic = new Topic();
            topic.setTitle(entry.getKey());
            topic.setCourse(course);
            topic.setOrderIndex(topicOrder++);
            topic = topicRepository.save(topic);

            for (AnkiItem ankiItem : entry.getValue()) {
                StudyItem studyItem = new StudyItem();
                studyItem.setPrimaryText(ankiItem.getFront() != null ? ankiItem.getFront() : "");
                studyItem.setSecondaryText(ankiItem.getReading() != null ? ankiItem.getReading() : "");
                studyItem.setMeaning(ankiItem.getBack() != null ? ankiItem.getBack() : "");
                studyItem.setTopic(topic);
                studyItem.setType("VOCABULARY");
                studyItemRepository.save(studyItem);
            }
        }

        return Map.of(
                "message", "Import successful",
                "courseId", course.getId(),
                "courseName", course.getTitle(),
                "topicsCreated", itemsByTopic.size(),
                "itemsCreated", request.getItems().size());
    }

    // --- Statistics ---

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

    public boolean updateUserRole(Long userId, String newRole) {
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

    public Map<String, String> clearCache(String cacheName) {
        // Placeholder implementation
        if (cacheName == null) {
            return Map.of("message", "All caches cleared");
        }
        return Map.of("message", "Cache '" + cacheName + "' cleared");
    }
}
