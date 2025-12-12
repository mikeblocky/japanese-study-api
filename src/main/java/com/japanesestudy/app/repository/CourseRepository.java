package com.japanesestudy.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.japanesestudy.app.dto.AdminCourseSummary;
import com.japanesestudy.app.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query(
            "select c.id as id, "
            + "c.title as title, "
            + "c.description as description, "
            + "c.level as level, "
            + "count(distinct t.id) as topicCount, "
            + "count(si.id) as itemCount "
            + "from Course c "
            + "left join c.topics t "
            + "left join t.studyItems si "
            + "group by c.id, c.title, c.description, c.level "
            + "order by c.id")
    List<AdminCourseSummary> findAdminCourseSummaries();
}
