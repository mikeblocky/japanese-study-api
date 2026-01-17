package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.progress.ProgressResponse;
import com.japanesestudy.app.dto.progress.ProgressStatsResponse;
import com.japanesestudy.app.dto.progress.RecordProgressRequest;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * REST controller for user progress tracking.
 * All endpoints are automatically scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    /**
     * Get all progress for the current user.
     */
    @GetMapping
    public ResponseEntity<List<ProgressResponse>> getAllProgress(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ProgressResponse> progress = progressService.getAllProgress(userDetails.getId());
        return ResponseEntity.ok(progress);
    }

    /**
     * Get progress for items in a specific topic.
     */
    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<ProgressResponse>> getTopicProgress(
            @PathVariable Long topicId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ProgressResponse> progress = progressService.getTopicProgress(userDetails.getId(), topicId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Get aggregated statistics for the current user.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ProgressStatsResponse stats = progressService.getStats(userDetails.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Record a study result.
     */
    @PostMapping("/record")
    public ResponseEntity<ProgressResponse> recordProgress(
            @RequestBody RecordProgressRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ProgressResponse progress = progressService.recordProgress(
                userDetails.getId(),
                request.getStudyItemId(),
                request.isCorrect(),
                request.isHarshMode());
        return ResponseEntity.ok(progress);
    }

    /**
     * Get items for challenge mode (random studied items).
     */
    @GetMapping("/challenge")
    public ResponseEntity<List<ProgressResponse>> getChallengeItems(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ProgressResponse> items = progressService.getChallengeItems(userDetails.getId(), limit);
        return ResponseEntity.ok(items);
    }
}
