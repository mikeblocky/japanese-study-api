package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.Mastery;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.repository.MasteryRepository;
import com.japanesestudy.app.repository.UserProgressRepository;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final MasteryRepository masteryRepository;

    private final UserProgressRepository userProgressRepository;

    public SessionController(MasteryRepository masteryRepository, UserProgressRepository userProgressRepository) {
        this.masteryRepository = masteryRepository;
        this.userProgressRepository = userProgressRepository;
    }

    @GetMapping("/mastery")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Mastery> getMastery(@RequestParam long userId) {
        return masteryRepository.findByUserId(userId);
    }

    @GetMapping("/due")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<UserProgress> getDueItems(@RequestParam long userId) {
        return userProgressRepository.findDueItems(userId, java.time.LocalDateTime.now());
    }
}
