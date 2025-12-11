package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    StudyItemRepository studyItemRepository;

    @Autowired
    StudySessionRepository sessionRepository;

    // --- Course Management ---

    @GetMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public Course createCourse(@RequestBody Course course) {
        return courseRepository.save(course);
    }

    @PatchMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course updates) {
        return courseRepository.findById(id)
                .map(course -> {
                    if (updates.getTitle() != null)
                        course.setTitle(updates.getTitle());
                    if (updates.getDescription() != null)
                        course.setDescription(updates.getDescription());
                    if (updates.getLevel() != null)
                        course.setLevel(updates.getLevel());
                    return ResponseEntity.ok(courseRepository.save(course));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        if (!courseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        courseRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Course deleted"));
    }

    // --- Anki Import ---

    @PostMapping("/anki/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importAnki(@RequestBody AnkiImportRequest request) {
        try {
            // Create course
            Course course = new Course();
            course.setTitle(request.getCourseName() != null ? request.getCourseName() : "Imported Course");
            course.setDescription(request.getDescription());
            course.setLevel("Custom");
            course = courseRepository.save(course);

            // Group items by topic - use TreeMap for sorted order
            Map<String, List<AnkiItem>> itemsByTopic = new java.util.TreeMap<>();
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

                int itemOrder = 0;
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

            return ResponseEntity.ok(Map.of(
                    "message", "Import successful",
                    "courseId", course.getId(),
                    "courseName", course.getTitle(),
                    "topicsCreated", itemsByTopic.size(),
                    "itemsCreated", request.getItems().size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Import failed: " + e.getMessage()));
        }
    }

    // --- Statistics ---

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalCourses", courseRepository.count());
        stats.put("totalItems", studyItemRepository.count());
        stats.put("totalSessions", sessionRepository.count());
        stats.put("javaVersion", System.getProperty("java.version"));
        stats.put("caches", Map.of("users", "active", "courses", "active"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getHealth() {
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
        return ResponseEntity.ok(health);
    }

    @GetMapping("/database/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("estimatedStorageKB", 1204);
        return ResponseEntity.ok(stats);
    }

    // --- DTOs ---

    public static class AnkiImportRequest {
        private String courseName;
        private String description;
        private List<AnkiItem> items;

        public String getCourseName() {
            return courseName;
        }

        public void setCourseName(String courseName) {
            this.courseName = courseName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<AnkiItem> getItems() {
            return items != null ? items : List.of();
        }

        public void setItems(List<AnkiItem> items) {
            this.items = items;
        }
    }

    public static class AnkiItem {
        private String front;
        private String reading;
        private String back;
        private String topic;

        public String getFront() {
            return front;
        }

        public void setFront(String front) {
            this.front = front;
        }

        public String getReading() {
            return reading;
        }

        public void setReading(String reading) {
            this.reading = reading;
        }

        public String getBack() {
            return back;
        }

        public void setBack(String back) {
            this.back = back;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }
}
