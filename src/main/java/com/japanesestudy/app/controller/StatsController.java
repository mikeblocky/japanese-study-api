package com.japanesestudy.app.controller;

import com.japanesestudy.app.repository.StudySessionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StudySessionRepository studySessionRepository;

    public StatsController(StudySessionRepository studySessionRepository) {
        this.studySessionRepository = studySessionRepository;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Map<String, Object> getStatsSummary() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDuration", 0);
        stats.put("totalSessions", studySessionRepository.count());
        stats.put("currentStreak", 0);
        stats.put("recentActivity", List.of());
        return stats;
    }
}
