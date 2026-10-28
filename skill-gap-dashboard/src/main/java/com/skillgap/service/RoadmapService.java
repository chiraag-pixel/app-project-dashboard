package com.skillgap.service;

import com.skillgap.dto.RoadmapTaskForm;
import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.GapAnalysis;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.LearningRoadmap;
import com.skillgap.entity.LearningResource;
import com.skillgap.entity.RoadmapTask;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.Priority;
import com.skillgap.entity.enums.TaskStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.LearningResourceRepository;
import com.skillgap.repository.LearningRoadmapRepository;
import com.skillgap.repository.RoadmapTaskRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Learning roadmap: generate tasks from real gaps, track completion. */
@Service
public class RoadmapService {

    private final LearningRoadmapRepository roadmapRepository;
    private final RoadmapTaskRepository taskRepository;
    private final GapAnalysisService gapAnalysisService;
    private final LearningResourceRepository resourceRepository;

    public RoadmapService(LearningRoadmapRepository roadmapRepository,
                          RoadmapTaskRepository taskRepository,
                          GapAnalysisService gapAnalysisService,
                          LearningResourceRepository resourceRepository) {
        this.roadmapRepository = roadmapRepository;
        this.taskRepository = taskRepository;
        this.gapAnalysisService = gapAnalysisService;
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public LearningRoadmap currentRoadmap(User user) {
        return roadmapRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
    }

    public List<RoadmapTask> tasksOf(Long roadmapId) {
        return taskRepository.findByRoadmapIdOrderByPlannedDateAscIdAsc(roadmapId);
    }

    public RoadmapTask getTaskOwned(User user, Long taskId) {
        return taskRepository.findByIdAndRoadmapUserId(taskId, user.getId())
                .orElseThrow(() -> new NotFoundException("Roadmap task not found"));
    }

    /** Progress = completed / total, or 0 when the roadmap is empty. */
    public double progressPercent(Long roadmapId) {
        long total = taskRepository.countByRoadmapId(roadmapId);
        if (total == 0) {
            return 0.0;
        }
        long done = taskRepository.countByRoadmapIdAndStatus(roadmapId, TaskStatus.COMPLETED);
        return Math.round(done / (double) total * 1000.0) / 10.0;
    }

    /**
     * Creates (replaces) the user's roadmap from the gaps of a saved analysis,
     * ordered by priority. Each task links a real learning resource when one
     * exists in the catalog.
     */
    @Transactional
    public LearningRoadmap generateFromAnalysis(User user, AnalysisSession session) {
        GapAnalysis analysis = gapAnalysisService.getAnalysisForSession(session.getId());
        List<GapAnalysisDetail> gaps = gapAnalysisService.getDetails(analysis.getId()).stream()
                .filter(d -> d.getPriority() != Priority.MET)
                .sorted(Comparator
                        .comparing((GapAnalysisDetail d) -> d.getPriority().ordinal()).reversed()
                        .thenComparing(d -> d.getGapPercentage(), Comparator.reverseOrder()))
                .toList();
        if (gaps.isEmpty()) {
            throw new BusinessException("No open skill gaps for this analysis - nothing to plan!");
        }

        LearningRoadmap roadmap = new LearningRoadmap(user, "Learning plan for " + session.getTargetLabel());
        roadmap.setTargetLabel(session.getTargetLabel());
        roadmapRepository.save(roadmap);

        LocalDate start = LocalDate.now();
        int week = 0;
        for (GapAnalysisDetail gap : gaps) {
            RoadmapTask task = new RoadmapTask(roadmap, buildTitle(gap));
            task.setSkill(gap.getSkill());
            task.setPriority(gap.getPriority());
            task.setPlannedDate(start.plusWeeks(week++));
            List<LearningResource> resources = resourceRepository.findByActiveTrueAndSkillId(gap.getSkill().getId());
            if (!resources.isEmpty()) {
                LearningResource resource = resources.get(0);
                task.setResource(resource);
                task.setUrl(resource.getUrl());
                task.setDifficulty(resource.getDifficulty());
                task.setEstimatedHours(resource.getEstimatedHours());
            } else {
                task.setDifficulty(Math.min(5, Math.max(1, gap.getRequiredLevel())));
            }
            roadmap.addTask(task);
            taskRepository.save(task);
        }
        return roadmap;
    }

    private String buildTitle(GapAnalysisDetail gap) {
        String current = gap.getCurrentLevel() == null ? "unassessed" : "level " + gap.getCurrentLevel();
        return switch (gap.getPriority()) {
            case CRITICAL -> "Close critical gap: " + gap.getSkill().getName()
                    + " (" + current + " → " + gap.getRequiredLevel() + ")";
            case HIGH, MEDIUM -> "Improve " + gap.getSkill().getName()
                    + " to level " + gap.getRequiredLevel();
            case LOW -> "Brush up " + gap.getSkill().getName();
            case MET -> "Maintain " + gap.getSkill().getName();
        };
    }

    // --------------------------------------------------------------------- tasks

    @Transactional
    public RoadmapTask addTask(User user, RoadmapTaskForm form) {
        LearningRoadmap roadmap = currentRoadmap(user);
        if (roadmap == null) {
            roadmap = roadmapRepository.save(new LearningRoadmap(user, "My Learning Roadmap"));
        }
        RoadmapTask task = new RoadmapTask(roadmap, form.getTitle().trim());
        applyForm(task, form);
        return taskRepository.save(task);
    }

    @Transactional
    public RoadmapTask updateTask(User user, Long taskId, RoadmapTaskForm form) {
        RoadmapTask task = getTaskOwned(user, taskId);
        task.setTitle(form.getTitle().trim());
        applyForm(task, form);
        return taskRepository.save(task);
    }

    private void applyForm(RoadmapTask task, RoadmapTaskForm form) {
        task.setUrl(trimToNull(form.getUrl()));
        task.setDifficulty(form.getDifficulty() == null ? 3 : form.getDifficulty());
        task.setEstimatedHours(form.getEstimatedHours());
        task.setPriority(form.getPriority() == null ? Priority.MEDIUM : form.getPriority());
        task.setPlannedDate(form.getPlannedDate());
        if (form.getStatus() != null) {
            task.setStatus(form.getStatus());
        }
    }

    @Transactional
    public void changeStatus(User user, Long taskId, TaskStatus status) {
        RoadmapTask task = getTaskOwned(user, taskId);
        task.setStatus(status);
        taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(User user, Long taskId) {
        RoadmapTask task = getTaskOwned(user, taskId);
        taskRepository.delete(task);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
