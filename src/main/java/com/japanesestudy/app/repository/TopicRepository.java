package com.japanesestudy.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.entity.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(Long courseId);

    Page<Topic> findByCourseIdAndDeletedFalse(Long courseId, Pageable pageable);

    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.course WHERE t.id = :id")
    Optional<Topic> findByIdWithCourse(Long id);

    boolean existsByCourseIdAndTitleIgnoreCaseAndDeletedFalse(Long courseId, String title);

    boolean existsByCourseIdAndTitleIgnoreCaseAndIdNotAndDeletedFalse(Long courseId, String title, Long id);

    @Query("SELECT COALESCE(MAX(t.orderIndex), -1) FROM Topic t WHERE t.course.id = :courseId AND t.deleted = false")
    int findMaxOrderIndexByCourseId(Long courseId);

    long countByCourseIdAndDeletedFalse(Long courseId);

    long countByDeletedFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Topic t SET t.deleted = true WHERE t.course.id = :courseId")
    int softDeleteByCourseId(Long courseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM topics WHERE course_id = :courseId", nativeQuery = true)
    int hardDeleteByCourseId(Long courseId);

}
