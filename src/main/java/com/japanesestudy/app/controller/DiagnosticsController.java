package com.japanesestudy.app.controller;

import com.japanesestudy.app.entity.UserProgress;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
public class DiagnosticsController {

    private final UserProgressRepository progressRepository;
    private final UserRepository userRepository;

    @GetMapping("/dump")
    public ResponseEntity<?> dumpProgress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String username) {
        
        Long userId = null;
        if (userDetails != null) {
            userId = userDetails.getId();
        } else if (username != null) {
            var user = userRepository.findByUsername(username).orElse(null);
            if (user != null) userId = user.getId();
        }

        if (userId == null) return ResponseEntity.status(401).body("No user found. Provide ?username=YOUR_NAME");
        
        List<UserProgress> all = progressRepository.findByUserId(userId);
        long countTrue = all.stream().filter(p -> Boolean.TRUE.equals(p.getStudied())).count();
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "totalRows", all.size(),
            "countStudiedTrue_Java", countTrue,
            "rows", all.stream().map(p -> Map.of(
                "itemId", p.getStudyItem().getId(),
                "studied", (Object)p.getStudied(),
                "lastStudied", p.getLastStudied() != null ? p.getLastStudied().toString() : "null"
            )).limit(10).collect(Collectors.toList())
        ));
    }
}
