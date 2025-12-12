package com.japanesestudy.app.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.japanesestudy.app.entity.StudyItem;

public interface StudyItemRepository extends JpaRepository<StudyItem, Long> {

    List<StudyItem> findByTopicId(long topicId);

    Page<StudyItem> findByTopicId(long topicId, Pageable pageable);
}
