package com.skillgap.repository;

import com.skillgap.entity.AnalysisSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisSessionRepository extends JpaRepository<AnalysisSession, Long> {

    List<AnalysisSession> findByUserIdOrderByAnalyzedAtDesc(Long userId);

    Optional<AnalysisSession> findByIdAndUserId(Long id, Long userId);

    Optional<AnalysisSession> findTopByUserIdOrderByAnalyzedAtDesc(Long userId);
}
