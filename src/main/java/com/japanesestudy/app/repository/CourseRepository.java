package com.japanesestudy.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.Visibility;

public interface CourseRepository extends JpaRepository<Course, Long> {
    
    // Find all public courses
    List<Course> findByVisibility(Visibility visibility);
    
    // Find courses owned by a specific user (use underscore to traverse relationship)
    List<Course> findByOwner_Id(Long ownerId);
    
    // Find public courses OR courses owned by a specific user
    @Query("SELECT c FROM Course c WHERE c.visibility = :visibility OR c.owner.id = :ownerId")
    List<Course> findByVisibilityOrOwnerId(@Param("visibility") Visibility visibility, @Param("ownerId") Long ownerId);
}

