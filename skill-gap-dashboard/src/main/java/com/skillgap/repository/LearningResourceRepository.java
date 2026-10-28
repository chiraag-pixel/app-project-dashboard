package com.skillgap.repository;

import com.skillgap.entity.LearningResource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    List<LearningResource> findByActiveTrue();

    List<LearningResource> findByActiveTrueAndSkillId(Long skillId);

    List<LearningResource> findAllByOrderByTitleAsc();
}
