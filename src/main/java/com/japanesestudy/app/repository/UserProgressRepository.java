package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    List<UserProgress> findByUserId(Long userId);
    Optional<UserProgress> findByUserIdAndStudyItemId(Long userId, Long studyItemId);

    @Query("SELECT p FROM UserProgress p WHERE p.user.id = :userId AND p.studyItem.topic.id = :topicId")
    List<UserProgress> findByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @Query("SELECT p FROM UserProgress p WHERE p.user.id = :userId AND p.nextReviewDate <= :now ORDER BY p.nextReviewDate")
    List<UserProgress> findDueForReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
