package com.japanesestudy.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.japanesestudy.app.entity.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    @Query("SELECT t FROM Topic t LEFT JOIN FETCH t.course WHERE t.id = :id")
    Optional<Topic> findByIdWithCourse(Long id);

    long countByCourseId(Long courseId);

}
