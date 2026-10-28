package com.skillgap.repository;

import com.skillgap.entity.LearningRoadmap;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningRoadmapRepository extends JpaRepository<LearningRoadmap, Long> {

    Optional<LearningRoadmap> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<LearningRoadmap> findByUserIdOrderByCreatedAtDesc(Long userId);
}
