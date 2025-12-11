package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.Goal;
import com.japanesestudy.app.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    @Autowired
    GoalRepository goalRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Goal> getGoals() {
        return goalRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Goal createGoal(@RequestBody Goal goal) {
        if (goal.getTargetDate() == null) {
            goal.setTargetDate(LocalDate.now());
        }
        return goalRepository.save(goal);
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Goal> toggleGoal(@PathVariable Long id) {
        return goalRepository.findById(id).map(goal -> {
            goal.setCompleted(!goal.isCompleted());
            return ResponseEntity.ok(goalRepository.save(goal));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id) {
        if (!goalRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        goalRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
