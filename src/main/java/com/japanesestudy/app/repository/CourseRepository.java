package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitle(String title);
    List<Course> findByTitleAndOwner_Id(String title, Long ownerId);
    Optional<Course> findByTitleAndOwnerId(String title, Long ownerId);
    
    @Query("SELECT c FROM Course c WHERE c.owner.id = ?1")
    List<Course> findByOwnerId(Long ownerId);
}
