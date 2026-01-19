package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.progress.ProgressDtos.*;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<ProgressResponse>> getTopicProgress(
            @PathVariable Long topicId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(progressService.getTopicProgress(userDetails.getId(), topicId));
    }

    @PostMapping("/record")
    public ResponseEntity<ProgressResponse> recordProgress(
            @RequestBody RecordProgressRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(progressService.recordProgress(
                userDetails.getId(), 
                request.getStudyItemId(), 
                request.isCorrect(), 
                request.isHarshMode()));
    }

    @GetMapping("/challenge")
    public ResponseEntity<List<ProgressResponse>> getChallengeItems(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(progressService.getChallengeItems(userDetails.getId(), limit));
    }

    @GetMapping("/stats")
    public ResponseEntity<ProgressStatsResponse> getStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(progressService.getStats(userDetails.getId()));
    }
}
