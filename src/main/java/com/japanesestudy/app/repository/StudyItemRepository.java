package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.StudyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyItemRepository extends JpaRepository<StudyItem, Long> {
    List<StudyItem> findByTopicId(Long topicId);
}
