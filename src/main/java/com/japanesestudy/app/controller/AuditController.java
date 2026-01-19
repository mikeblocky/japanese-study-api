package com.japanesestudy.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.japanesestudy.app.entity.AuditLog;
import com.japanesestudy.app.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<AuditLog>> listRecent(@RequestParam(defaultValue = "50") int limit) {
        int capped = Math.min(Math.max(limit, 1), 200);
        List<AuditLog> logs = auditLogRepository.findAll(org.springframework.data.domain.PageRequest.of(0, capped,
                org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();
        return ResponseEntity.ok(logs);
    }
}
