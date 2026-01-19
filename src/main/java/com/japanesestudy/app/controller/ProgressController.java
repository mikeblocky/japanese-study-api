package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.dto.progress.ProgressDtos.ProgressResponse;
import com.japanesestudy.app.dto.progress.ProgressDtos.ProgressStatsResponse;
import com.japanesestudy.app.dto.progress.ProgressDtos.RecordProgressRequest;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.ProgressService;

import lombok.RequiredArgsConstructor;

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

    @GetMapping("/studied")
    public ResponseEntity<List<ProgressResponse>> getStudiedItems(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<ProgressResponse> progress = progressService.getAllProgress(userDetails.getId()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getStudied()))
                .toList();
        return ResponseEntity.ok(progress);
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
