package com.japanesestudy.app.controller;

import com.japanesestudy.app.model.StudyItem;
import com.japanesestudy.app.service.StudyContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily")
@CrossOrigin(origins = "http://localhost:5173")
public class DailyCardsController {

    @Autowired
    private StudyContentService contentService;

    @GetMapping("/cards")
    public ResponseEntity<List<StudyItem>> getDailyCards(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "20") int count) {
        List<StudyItem> dailyCards = contentService.getDailyReviewCards(userId, count);
        return ResponseEntity.ok(dailyCards);
    }
}
