package com.japanesestudy.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.japanesestudy.app.entity.StudyItem;

@Repository
public interface StudyItemRepository extends JpaRepository<StudyItem, Long> {

    List<StudyItem> findByTopicId(Long topicId);

    Page<StudyItem> findByTopicId(Long topicId, Pageable pageable);

    long countByTopicId(Long topicId);

    @Query("SELECT COUNT(si) FROM StudyItem si WHERE si.topic.course.id = :courseId")
    long countByCourseId(Long courseId);
}
