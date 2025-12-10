package com.japanesestudy.app.controller;

import com.japanesestudy.app.model.Goal;
import com.japanesestudy.app.model.User;
import com.japanesestudy.app.repository.GoalRepository;
import com.japanesestudy.app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyGoalController {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public StudyGoalController(GoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    // Helper to get current user (mocked for now as id 1)
    private User getCurrentUser() {
        return userRepository.findById(1L).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<Goal> getGoals() {
        User user = getCurrentUser();
        return goalRepository.findByUserId(user.getId());
    }

    @PostMapping
    public Goal createGoal(@RequestBody Goal goal) {
        User user = getCurrentUser();
        goal.setUser(user);
        if (goal.getIsCompleted() == null) {
            goal.setIsCompleted(false);
        }
        if (goal.getTargetDate() == null) {
            goal.setTargetDate(LocalDate.now());
        }
        return goalRepository.save(goal);
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Goal> toggleGoal(@PathVariable Long id) {
        Goal goal = goalRepository.findById(id).orElseThrow(() -> new RuntimeException("Goal not found"));
        // Security check omitted for simplicity (should check user ownership)
        goal.setIsCompleted(!goal.getIsCompleted());
        return ResponseEntity.ok(goalRepository.save(goal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
