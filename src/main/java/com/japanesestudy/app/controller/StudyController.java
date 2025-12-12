package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.dto.SubmitAnswerRequest;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.StudySession;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.StudyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @GetMapping("/items/due")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<UserProgress> getDueItems(@AuthenticationPrincipal @NonNull UserDetailsImpl userDetails) {
        return studyService.getDueItems(userDetails.getId());
    }

    @GetMapping("/items/topic/{topicId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<StudyItem> getItemsByTopic(@PathVariable long topicId) {
        return studyService.getItemsByTopic(topicId);
    }

    @PostMapping("/session/start")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StudySession> startSession(@AuthenticationPrincipal @NonNull UserDetailsImpl userDetails) {
        return ResponseEntity.ok(studyService.startSession(userDetails.getId()));
    }

    @PostMapping("/session/{sessionId}/submit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> submitAnswer(@PathVariable long sessionId,
            @Valid @NonNull @RequestBody SubmitAnswerRequest request) {
        studyService.submitAnswer(sessionId, request.getItemId(), request.isCorrect());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{sessionId}/end")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StudySession> endSession(@PathVariable long sessionId) {
        return ResponseEntity.ok(studyService.endSession(sessionId));
    }
}
