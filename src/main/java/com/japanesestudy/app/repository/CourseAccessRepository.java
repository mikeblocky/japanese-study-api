package com.japanesestudy.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.japanesestudy.app.entity.CourseAccess;

@Repository
public interface CourseAccessRepository extends JpaRepository<CourseAccess, Long> {

    Optional<CourseAccess> findByCourseIdAndUserId(Long courseId, Long userId);

    List<CourseAccess> findByCourseId(Long courseId);
}
