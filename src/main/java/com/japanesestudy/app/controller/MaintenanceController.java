package com.japanesestudy.app.controller;

import com.japanesestudy.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final UserProgressRepository userProgressRepository;

    @DeleteMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> resetAllData() {
        // Order matters for foreign keys
        userProgressRepository.deleteAll();
        studyItemRepository.deleteAll();
        topicRepository.deleteAll();
        courseRepository.deleteAll();

        return ResponseEntity.ok(Map.of(
            "message", "All course data and progress have been cleared successfully.",
            "status", "success"
        ));
    }
}
