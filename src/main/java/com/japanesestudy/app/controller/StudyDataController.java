package com.japanesestudy.app.controller;

import com.japanesestudy.app.model.*;
import com.japanesestudy.app.service.StudyContentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "http://localhost:5173") // Allow Vite dev server
public class StudyDataController {
    private final StudyContentService contentService;
    private final com.japanesestudy.app.service.AnkiImportService ankiImportService;

    public StudyDataController(StudyContentService contentService,
            com.japanesestudy.app.service.AnkiImportService ankiImportService) {
        this.contentService = contentService;
        this.ankiImportService = ankiImportService;
    }

    @GetMapping("/courses")
    public List<Course> getAllCourses() {
        return contentService.getAllCourses();
    }

    @PostMapping("/courses")
    public Course createCourse(@RequestBody Course course) {
        return contentService.createCourse(course);
    }

    @GetMapping("/courses/{courseId}/topics")
    public List<Topic> getTopics(@PathVariable Long courseId) {
        return contentService.getTopicsByCourse(courseId);
    }

    @PostMapping("/topics")
    public Topic createTopic(@RequestBody Topic topic) {
        return contentService.createTopic(topic);
    }

    @GetMapping("/topics/{topicId}/items")
    public List<StudyItem> getItems(@PathVariable Long topicId) {
        return contentService.getItemsByTopic(topicId);
    }

    @GetMapping("/types")
    public List<ItemType> getItemTypes() {
        return contentService.getAllItemTypes();
    }

    @PostMapping("/items")
    public StudyItem createItem(@RequestBody StudyItem item) {
        return contentService.createStudyItem(item);
    }

    @PostMapping("/items/batch")
    public List<StudyItem> createItemsBatch(@RequestBody List<StudyItem> items) {
        return contentService.createStudyItems(items);
    }

    @PutMapping("/items/{id}")
    public StudyItem updateItem(@PathVariable Long id, @RequestBody StudyItem item) {
        return contentService.updateStudyItem(id, item);
    }

    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable Long id) {
        contentService.deleteStudyItem(id);
    }

    @GetMapping("/test-import")
    public String triggerImport() {
        ankiImportService.importAnkiPackage("data/Japanese_Minna_no_Nihongo_1__2_Lessons_1_-_50.apkg");
        return "Import triggered check logs";
    }
}
