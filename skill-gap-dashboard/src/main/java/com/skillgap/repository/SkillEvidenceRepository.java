package com.skillgap.repository;

import com.skillgap.entity.SkillEvidence;
import com.skillgap.entity.enums.VerificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, Long> {

    List<SkillEvidence> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SkillEvidence> findByExtractionRunIdOrderBySkillNameAsc(Long extractionRunId);

    Optional<SkillEvidence> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndVerificationStatus(Long userId, VerificationStatus status);
}
