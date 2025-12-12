package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findByUserIdOrderByStartTimeDesc(long userId);

    long countByUserId(long userId);
}
