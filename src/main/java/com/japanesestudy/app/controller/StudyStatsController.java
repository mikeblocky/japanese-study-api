package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.StatsSummaryDTO;
import com.japanesestudy.app.model.User;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.service.StatsService;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for study statistics endpoints.
 * Simplified: delegates all business logic to StatsService.
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyStatsController {

    private final StatsService statsService;
    private final UserRepository userRepository;

    public StudyStatsController(StatsService statsService, UserRepository userRepository) {
        this.statsService = statsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    public StatsSummaryDTO getStatsSummary(
            @RequestParam(required = false, defaultValue = "1") Long userId) {
        User user = getUser(userId);
        return statsService.getStatsSummary(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }
}
