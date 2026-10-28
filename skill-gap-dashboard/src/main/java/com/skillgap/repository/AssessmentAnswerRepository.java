package com.skillgap.repository;

import com.skillgap.entity.AssessmentAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {

    List<AssessmentAnswer> findByAttemptId(Long attemptId);
}
