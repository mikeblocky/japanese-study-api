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

    /**
     * Find all progress records for a user.
     */
    List<UserProgress> findByUserId(Long userId);

    /**
     * Find progress for a specific user and study item.
     */
    Optional<UserProgress> findByUserIdAndStudyItemId(Long userId, Long studyItemId);

    /**
     * Find all progress for items in a specific topic.
     */
    @Query("SELECT p FROM UserProgress p WHERE p.user.id = :userId AND p.studyItem.topic.id = :topicId")
    List<UserProgress> findByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

    /**
     * Count total items studied by user.
     */
    long countByUserId(Long userId);

    /**
     * Count mastered items (masteryLevel >= 3) for a user.
     */
    @Query("SELECT COUNT(p) FROM UserProgress p WHERE p.user.id = :userId AND p.masteryLevel >= 3")
    long countMasteredByUserId(@Param("userId") Long userId);

    /**
     * Count fully mastered items (masteryLevel = 5) for a user.
     */
    @Query("SELECT COUNT(p) FROM UserProgress p WHERE p.user.id = :userId AND p.masteryLevel = 5")
    long countFullyMasteredByUserId(@Param("userId") Long userId);

    /**
     * Find items due for review (nextReviewDate <= now).
     */
    @Query("SELECT p FROM UserProgress p WHERE p.user.id = :userId AND p.nextReviewDate <= :now ORDER BY p.nextReviewDate")
    List<UserProgress> findDueForReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Get total correct answers for a user.
     */
    @Query("SELECT COALESCE(SUM(p.correctCount), 0) FROM UserProgress p WHERE p.user.id = :userId")
    long sumCorrectByUserId(@Param("userId") Long userId);

    /**
     * Get total incorrect answers for a user.
     */
    @Query("SELECT COALESCE(SUM(p.incorrectCount), 0) FROM UserProgress p WHERE p.user.id = :userId")
    long sumIncorrectByUserId(@Param("userId") Long userId);
}
