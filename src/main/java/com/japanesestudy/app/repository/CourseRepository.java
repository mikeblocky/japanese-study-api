package com.japanesestudy.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.japanesestudy.app.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTitle(String title);

    List<Course> findByTitleAndOwner_Id(String title, Long ownerId);
}
