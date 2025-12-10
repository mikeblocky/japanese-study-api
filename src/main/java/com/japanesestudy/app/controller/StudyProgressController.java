package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.TopicProgressDTO;
import com.japanesestudy.app.model.User;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.service.ProgressService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for study progress endpoints.
 * Simplified: delegates all business logic to ProgressService.
 */
@RestController
@RequestMapping("/api/progress")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyProgressController {

        private final ProgressService progressService;
        private final UserRepository userRepository;

        public StudyProgressController(ProgressService progressService, UserRepository userRepository) {
                this.progressService = progressService;
                this.userRepository = userRepository;
        }

        @GetMapping("/topics/{topicId}")
        public TopicProgressDTO getTopicProgress(
                        @PathVariable Long topicId,
                        @RequestParam(required = false, defaultValue = "1") Long userId) {
                User user = getUser(userId);
                return progressService.getTopicProgress(topicId, user);
        }

        @GetMapping("/summary")
        public List<TopicProgressDTO> getAllTopicProgress(
                        @RequestParam(required = false, defaultValue = "1") Long userId) {
                User user = getUser(userId);
                return progressService.getAllTopicProgress(user);
        }

        private User getUser(Long userId) {
                return userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        }
}
