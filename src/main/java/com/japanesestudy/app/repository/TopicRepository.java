package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByCourseIdOrderByOrderIndexAsc(long courseId);
}
