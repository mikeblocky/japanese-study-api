package com.japanesestudy.app.controller;

import com.japanesestudy.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
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
    @Transactional
    public ResponseEntity<?> resetAllData() {
        userProgressRepository.deleteAll();
        studyItemRepository.deleteAll();
        topicRepository.deleteAll();
        courseRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "All data cleared", "status", "success"));
    }
}
