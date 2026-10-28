package com.skillgap.repository;

import com.skillgap.entity.AssessmentQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {

    List<AssessmentQuestion> findByAssessmentIdOrderBySortOrderAsc(Long assessmentId);
}
