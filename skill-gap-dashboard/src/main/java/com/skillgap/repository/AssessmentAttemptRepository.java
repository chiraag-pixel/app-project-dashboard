package com.skillgap.repository;

import com.skillgap.entity.AssessmentAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {

    List<AssessmentAttempt> findByUserIdOrderByCompletedAtDesc(Long userId);

    Optional<AssessmentAttempt> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("select coalesce(avg(a.scorePercent), 0) from AssessmentAttempt a where a.user.id = :userId and a.completedAt is not null")
    double averageScoreForUser(@Param("userId") Long userId);

}
