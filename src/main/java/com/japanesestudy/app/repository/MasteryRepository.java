package com.japanesestudy.app.repository;

import com.japanesestudy.app.entity.Mastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasteryRepository extends JpaRepository<Mastery, Long> {
    java.util.List<Mastery> findAllByUserIdAndNextReviewAtLessThanEqual(Long userId, java.time.LocalDateTime date);

    java.util.Optional<Mastery> findByUserIdAndItemId(Long userId, Long itemId);

    java.util.List<Mastery> findByUserId(Long userId);
}
