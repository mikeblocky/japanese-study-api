package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {
    List<SessionLog> findBySessionId(Long sessionId);
}
