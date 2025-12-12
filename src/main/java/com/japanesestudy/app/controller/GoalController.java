package com.japanesestudy.app.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.Goal;
import com.japanesestudy.app.repository.GoalRepository;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalRepository goalRepository;

    public GoalController(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<Goal> getGoals() {
        return goalRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Goal createGoal(@NonNull @RequestBody Goal goal) {
        if (goal.getTargetDate() == null) {
            goal.setTargetDate(LocalDate.now());
        }
        return goalRepository.save(goal);
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Goal> toggleGoal(@PathVariable long id) {
        return goalRepository.findById(id).map(goal -> {
            goal.setCompleted(!goal.isCompleted());
            return ResponseEntity.ok(goalRepository.save(goal));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteGoal(@PathVariable long id) {
        if (!goalRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        goalRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
