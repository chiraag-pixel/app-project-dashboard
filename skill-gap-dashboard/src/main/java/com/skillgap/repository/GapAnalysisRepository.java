package com.skillgap.repository;

import com.skillgap.entity.GapAnalysis;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GapAnalysisRepository extends JpaRepository<GapAnalysis, Long> {

    Optional<GapAnalysis> findBySessionId(Long sessionId);
}
