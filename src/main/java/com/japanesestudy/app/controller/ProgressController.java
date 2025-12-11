package com.japanesestudy.app.controller;

import com.japanesestudy.app.repository.UserProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired(required = false)
    UserProgressRepository userProgressRepository;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Map<String, Object>> getProgressSummary(@RequestParam Long userId) {
        // Return empty list for now, will be populated with real data
        return List.of();
    }
}
