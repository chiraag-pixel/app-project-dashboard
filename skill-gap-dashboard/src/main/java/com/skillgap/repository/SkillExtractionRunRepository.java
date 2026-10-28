package com.skillgap.repository;

import com.skillgap.entity.SkillExtractionRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillExtractionRunRepository extends JpaRepository<SkillExtractionRun, Long> {

    List<SkillExtractionRun> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SkillExtractionRun> findByIdAndUserId(Long id, Long userId);
}
