package com.japanesestudy.app.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Map<String, Object>> getProgressSummary(@RequestParam long userId) {
        // Return empty list for now, will be populated with real data
        return List.of();
    }
}
