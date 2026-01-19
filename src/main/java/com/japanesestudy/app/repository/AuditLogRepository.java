package com.japanesestudy.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.japanesestudy.app.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
