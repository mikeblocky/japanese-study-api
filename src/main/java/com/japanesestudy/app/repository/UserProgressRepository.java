package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByUserIdAndStudyItemId(Long userId, Long studyItemId);

    List<UserProgress> findByUserId(Long userId);

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = :userId AND up.nextReview <= :now")
    List<UserProgress> findDueItems(Long userId, LocalDateTime now);
}
