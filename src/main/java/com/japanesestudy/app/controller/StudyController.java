package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.SubmitAnswerRequest;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.StudySession;
import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @GetMapping("/items/due")
    public List<UserProgress> getDueItems(@RequestParam(name = "userId", required = false) Long userId) {
        long resolvedUserId = userId != null ? userId : 1L;
        return studyService.getDueItems(resolvedUserId);
    }

    @GetMapping("/items")
    public List<StudyItem> getAllItems() {
        return studyService.getAllItems();
    }

    @GetMapping("/items/topic/{topicId}")
    public List<StudyItem> getItemsByTopic(@PathVariable long topicId) {
        return studyService.getItemsByTopic(topicId);
    }

    @PostMapping("/session/start")
    public ResponseEntity<StudySession> startSession(@RequestParam(name = "userId", required = false) Long userId) {
        long resolvedUserId = userId != null ? userId : 1L;
        return ResponseEntity.ok(studyService.startSession(resolvedUserId));
    }

    @PostMapping("/session/{sessionId}/submit")
    public ResponseEntity<?> submitAnswer(@PathVariable long sessionId,
            @Valid @NonNull @RequestBody SubmitAnswerRequest request) {
        studyService.submitAnswer(sessionId, request.getItemId(), request.isCorrect());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<StudySession> endSession(@PathVariable long sessionId) {
        return ResponseEntity.ok(studyService.endSession(sessionId));
    }
}
