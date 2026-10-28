package com.skillgap.repository;

import com.skillgap.entity.AssessmentOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentOptionRepository extends JpaRepository<AssessmentOption, Long> {

    List<AssessmentOption> findByQuestionId(Long questionId);
}
