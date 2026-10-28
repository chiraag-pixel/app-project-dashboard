package com.skillgap.repository;

import com.skillgap.entity.GapAnalysisDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GapAnalysisDetailRepository extends JpaRepository<GapAnalysisDetail, Long> {

    List<GapAnalysisDetail> findByGapAnalysisIdOrderByGapDesc(Long gapAnalysisId);

    List<GapAnalysisDetail> findByGapAnalysisIdAndRequirementType(
            Long gapAnalysisId, com.skillgap.entity.enums.RequirementType requirementType);
}
