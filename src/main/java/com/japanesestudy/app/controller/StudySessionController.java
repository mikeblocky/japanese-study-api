package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.EndSessionRequestDTO;
import com.japanesestudy.app.dto.LogItemRequestDTO;
import com.japanesestudy.app.dto.TestRequestDTO;
import com.japanesestudy.app.model.*;
import com.japanesestudy.app.service.StudyActivityService;
import com.japanesestudy.app.service.UserService;
import com.japanesestudy.app.repository.StudyItemRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST controller for study session management.
 * Handles session lifecycle, test generation, and item logging.
 */
@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:5173")
public class StudySessionController {

    private final StudyActivityService activityService;
    private final UserService userService;
    private final StudyItemRepository itemRepository;

    public StudySessionController(StudyActivityService activityService,
            UserService userService,
            StudyItemRepository itemRepository) {
        this.activityService = activityService;
        this.userService = userService;
        this.itemRepository = itemRepository;
    }

    @GetMapping("/start")
    public StudySession startSession(@RequestParam Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        return activityService.startSession(user);
    }

    @PostMapping("/{sessionId}/end")
    public StudySession endSession(@PathVariable Long sessionId,
            @RequestBody(required = false) EndSessionRequestDTO request) {
        Long duration = request != null ? request.durationSeconds() : null;
        return activityService.endSession(sessionId, duration);
    }

    @PostMapping("/test/generate")
    public List<StudyItem> generateTest(@RequestBody TestRequestDTO request) {
        return activityService.generateTest(request.topicIds(), request.count());
    }

    @PostMapping("/{sessionId}/log")
    public SessionLog logItem(@PathVariable Long sessionId, @RequestBody LogItemRequestDTO request) {
        StudyItem item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new RuntimeException("Item not found: " + request.itemId()));
        return activityService.logItem(sessionId, item, request.correct());
    }

    @GetMapping("/due")
    public List<StudyItem> getDueItems(@RequestParam Long userId) {
        User user = userService.getUserById(userId);
        if (user == null)
            return Collections.emptyList();
        return activityService.getDueItems(user);
    }

    @GetMapping("/mastery")
    public List<Mastery> getAllMastery(@RequestParam Long userId) {
        User user = userService.getUserById(userId);
        if (user == null)
            return Collections.emptyList();
        return activityService.getAllMastery(user);
    }

    @PostMapping("/mastery/reset")
    public void resetMastery(@RequestParam Long userId, @RequestParam Long itemId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            activityService.resetMastery(user, itemId);
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(@RequestParam Long userId) {
        User user = userService.getUserById(userId);
        if (user == null)
            return Collections.emptyMap();
        return activityService.getStats(user);
    }
}
