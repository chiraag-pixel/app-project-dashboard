package com.skillgap.repository;

import com.skillgap.entity.RoadmapTask;
import com.skillgap.entity.enums.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapTaskRepository extends JpaRepository<RoadmapTask, Long> {

    List<RoadmapTask> findByRoadmapIdOrderByPlannedDateAscIdAsc(Long roadmapId);

    Optional<RoadmapTask> findByIdAndRoadmapUserId(Long taskId, Long userId);

    long countByRoadmapId(Long roadmapId);

    long countByRoadmapIdAndStatus(Long roadmapId, TaskStatus status);
}
