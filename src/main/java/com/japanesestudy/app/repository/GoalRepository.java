package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserId(long userId);

    List<Goal> findByUserIdAndIsCompletedFalse(long userId);
}
