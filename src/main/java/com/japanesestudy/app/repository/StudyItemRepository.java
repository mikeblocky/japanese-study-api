package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.StudyItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyItemRepository extends JpaRepository<StudyItem, Long> {
    List<StudyItem> findByTopicId(Long topicId);
    Page<StudyItem> findByTopicId(Long topicId, Pageable pageable);
}
