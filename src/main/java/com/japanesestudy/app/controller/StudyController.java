package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.SubmitAnswerRequest;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.StudySession;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    @Autowired
    StudyService studyService;

    @GetMapping("/items/due")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<UserProgress> getDueItems(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return studyService.getDueItems(userDetails.getId());
    }

    @GetMapping("/items/topic/{topicId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<StudyItem> getItemsByTopic(@PathVariable Long topicId) {
        return studyService.getItemsByTopic(topicId);
    }

    @PostMapping("/session/start")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StudySession> startSession(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(studyService.startSession(userDetails.getId()));
    }

    @PostMapping("/session/{sessionId}/submit")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> submitAnswer(@PathVariable Long sessionId,
            @RequestBody SubmitAnswerRequest request) {
        studyService.submitAnswer(sessionId, request.getItemId(), request.isCorrect());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{sessionId}/end")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StudySession> endSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(studyService.endSession(sessionId));
    }
}
