package com.skillgap.repository;

import com.skillgap.entity.Assessment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByActiveTrue();

    List<Assessment> findBySkillId(Long skillId);

    List<Assessment> findAllByOrderByTitleAsc();
}
