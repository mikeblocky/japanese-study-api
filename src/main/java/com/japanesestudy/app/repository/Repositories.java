package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Repositories {

    public interface CourseRepository extends JpaRepository<Course, Long> {
        List<Course> findByTitle(String title);
        List<Course> findByTitleAndOwner_Id(String title, Long ownerId);
    }

    public interface TopicRepository extends JpaRepository<Topic, Long> {
        List<Topic> findByCourseIdOrderByOrderIndexAsc(long courseId);
    }

    public interface StudyItemRepository extends JpaRepository<StudyItem, Long> {
        List<StudyItem> findByTopicId(long topicId);
        Page<StudyItem> findByTopicId(long topicId, Pageable pageable);
    }

    @Repository
    public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
        List<UserProgress> findByUserId(Long userId);
        Optional<UserProgress> findByUserIdAndStudyItemId(Long userId, Long studyItemId);

        @Query("SELECT p FROM UserProgress p WHERE p.user.id = :userId AND p.studyItem.topic.id = :topicId")
        List<UserProgress> findByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

        @Query("SELECT p FROM UserProgress p WHERE p.user.id = :userId AND p.nextReviewDate <= :now ORDER BY p.nextReviewDate")
        List<UserProgress> findDueForReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    }

    public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);
        Boolean existsByUsername(String username);
    }
}
