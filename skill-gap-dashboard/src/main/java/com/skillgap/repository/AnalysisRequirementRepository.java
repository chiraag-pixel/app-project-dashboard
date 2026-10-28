package com.skillgap.repository;

import com.skillgap.entity.AnalysisRequirement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRequirementRepository extends JpaRepository<AnalysisRequirement, Long> {

    List<AnalysisRequirement> findByUserIdAndInputDocumentIdOrderBySkillNameAsc(Long userId, Long inputDocumentId);

    List<AnalysisRequirement> findByInputDocumentId(Long inputDocumentId);

    List<AnalysisRequirement> findByUserIdAndInputDocumentIdAndVerificationStatus(
            Long userId, Long inputDocumentId, com.skillgap.entity.enums.VerificationStatus status);

    void deleteByInputDocumentId(Long inputDocumentId);
}
