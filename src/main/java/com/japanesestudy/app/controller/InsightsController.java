package com.japanesestudy.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.dto.insights.InsightDtos.ManagementInsightsResponse;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.InsightsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @GetMapping("/management")
    public ResponseEntity<ManagementInsightsResponse> getManagementInsights(
            @RequestParam(defaultValue = "50") int activityLimit,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(insightsService.getManagementInsights(userId, activityLimit));
    }
}
