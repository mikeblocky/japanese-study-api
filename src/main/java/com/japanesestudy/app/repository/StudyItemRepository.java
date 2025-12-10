package com.japanesestudy.app.repository;

import com.japanesestudy.app.model.StudyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyItemRepository extends JpaRepository<StudyItem, Long> {
    // Optimized query - uses index on topic_id
    List<StudyItem> findByTopicId(Long topicId);

    // Count items per topic
    long countByTopicId(Long topicId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM StudyItem s ORDER BY function('RAND')")
    List<StudyItem> findRandom(org.springframework.data.domain.Pageable pageable);
}
