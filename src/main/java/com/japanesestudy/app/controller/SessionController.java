package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Mastery;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.repository.MasteryRepository;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.repository.StudySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    MasteryRepository masteryRepository;

    @Autowired(required = false)
    UserProgressRepository userProgressRepository;

    @Autowired(required = false)
    StudySessionRepository studySessionRepository;

    @GetMapping("/mastery")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Mastery> getMastery(@RequestParam Long userId) {
        return masteryRepository.findByUserId(userId);
    }

    @GetMapping("/due")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<UserProgress> getDueItems(@RequestParam Long userId) {
        if (userProgressRepository != null) {
            return userProgressRepository.findDueItems(userId, java.time.LocalDateTime.now());
        }
        return List.of();
    }
}
