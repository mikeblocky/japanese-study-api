package com.japanesestudy.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.japanesestudy.app.entity.StudyItem;

@Repository
public interface StudyItemRepository extends JpaRepository<StudyItem, Long> {

    List<StudyItem> findByTopicIdAndDeletedFalse(Long topicId);

    Page<StudyItem> findByTopicIdAndDeletedFalse(Long topicId, Pageable pageable);

    long countByTopicIdAndDeletedFalse(Long topicId);

    @Query("SELECT COUNT(si) FROM StudyItem si WHERE si.topic.course.id = :courseId AND si.deleted = false AND si.topic.deleted = false")
    long countActiveByCourseId(Long courseId);

    long countByDeletedFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE StudyItem si SET si.deleted = true WHERE si.id = :itemId")
    int softDeleteById(Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM StudyItem si WHERE si.topic.id = :topicId")
    int hardDeleteByTopicId(Long topicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM study_items WHERE topic_id IN (SELECT id FROM topics WHERE course_id = :courseId)", nativeQuery = true)
    int hardDeleteByCourseId(Long courseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE StudyItem si SET si.deleted = true WHERE si.topic.id = :topicId")
    int softDeleteByTopicId(Long topicId);
}
