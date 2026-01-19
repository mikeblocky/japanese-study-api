package com.japanesestudy.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.entity.UserProgress;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    List<UserProgress> findByUserId(Long userId);

    @Query("SELECT up FROM UserProgress up JOIN up.studyItem si WHERE up.user.id = :userId AND si.deleted = false")
    List<UserProgress> findActiveByUserId(Long userId);

    @Query("SELECT up FROM UserProgress up WHERE up.user.id = ?1 AND up.studyItem.topic.id = ?2")
    List<UserProgress> findByUserIdAndTopicId(Long userId, Long topicId);

    @Query("SELECT up FROM UserProgress up JOIN up.studyItem si WHERE up.user.id = :userId AND si.topic.id = :topicId AND si.deleted = false")
    List<UserProgress> findActiveByUserIdAndTopicId(Long userId, Long topicId);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user.id = :userId AND up.studyItem.topic.id = :topicId AND up.studied = true AND up.studyItem.deleted = false")
    long countStudiedByUserAndTopic(Long userId, Long topicId);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.user.id = :userId AND up.studyItem.topic.course.id = :courseId AND up.studied = true AND up.studyItem.deleted = false")
    long countStudiedByUserAndCourse(Long userId, Long courseId);

    Optional<UserProgress> findByUserIdAndStudyItemId(Long userId, Long studyItemId);

    @Query("SELECT up FROM UserProgress up JOIN up.studyItem si WHERE up.user.id = ?1 AND up.nextReviewDate <= ?2 AND si.deleted = false")
    List<UserProgress> findDueForReview(Long userId, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM UserProgress up WHERE up.studyItem.topic.id = :topicId")
    int deleteByTopicId(Long topicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM UserProgress up WHERE up.studyItem.topic.course.id = :courseId")
    int deleteByCourseId(Long courseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM user_progress WHERE study_item_id IN (SELECT id FROM study_items WHERE topic_id IN (SELECT id FROM topics WHERE course_id = :courseId))", nativeQuery = true)
    int hardDeleteByCourseId(Long courseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM UserProgress up WHERE up.studyItem.id = :studyItemId")
    int deleteByStudyItemId(Long studyItemId);
}
