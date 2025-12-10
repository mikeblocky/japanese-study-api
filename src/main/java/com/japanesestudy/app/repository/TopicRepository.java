package com.japanesestudy.app.repository;

import com.japanesestudy.app.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByCourseId(Long courseId);

    java.util.Optional<Topic> findByTitleIgnoreCase(String title);
}
