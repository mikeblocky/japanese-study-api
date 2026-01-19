package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    List<UserProgress> findByUserId(Long userId);
    
    @Query("SELECT up FROM UserProgress up WHERE up.user.id = ?1 AND up.studyItem.topic.id = ?2")
    List<UserProgress> findByUserIdAndTopicId(Long userId, Long topicId);
    
    Optional<UserProgress> findByUserIdAndStudyItemId(Long userId, Long studyItemId);
    
    @Query("SELECT up FROM UserProgress up WHERE up.user.id = ?1 AND up.nextReviewDate <= ?2")
    List<UserProgress> findDueForReview(Long userId, LocalDateTime now);
}
