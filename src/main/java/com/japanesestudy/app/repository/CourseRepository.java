package com.japanesestudy.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.japanesestudy.app.entity.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTitle(String title);

    List<Course> findByTitleAndOwner_Id(String title, Long ownerId);

    @Query("SELECT c FROM Course c WHERE c.owner.id = ?1")
    List<Course> findByOwnerId(Long ownerId);

    @Query("SELECT c FROM Course c "
            + "WHERE (:ownerId IS NULL OR c.owner.id = :ownerId) "
            + "AND (:level IS NULL OR c.level = :level) "
            + "AND (:tag IS NULL OR LOWER(c.tags) LIKE LOWER(CONCAT('%', :tag, '%'))) "
            + "AND (:q IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Course> search(Long ownerId, String level, String tag, String q);
}
