package com.skillgap.service;

import com.skillgap.dto.DashboardData;
import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.AssessmentAttempt;
import com.skillgap.entity.GapAnalysis;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.JobRole;
import com.skillgap.entity.LearningRoadmap;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.entity.enums.Priority;
import com.skillgap.repository.AssessmentAttemptRepository;
import com.skillgap.repository.RoadmapTaskRepository;
import com.skillgap.repository.UserSkillRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles the dashboard strictly from persisted user data. */
@Service
public class DashboardService {

    private final UserSkillRepository userSkillRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final GapAnalysisService gapAnalysisService;
    private final RoadmapService roadmapService;
    private final ProfileService profileService;
    private final RoadmapTaskRepository taskRepository;

    public DashboardService(UserSkillRepository userSkillRepository,
                            AssessmentAttemptRepository attemptRepository,
                            GapAnalysisService gapAnalysisService,
                            RoadmapService roadmapService,
                            ProfileService profileService,
                            RoadmapTaskRepository taskRepository) {
        this.userSkillRepository = userSkillRepository;
        this.attemptRepository = attemptRepository;
        this.gapAnalysisService = gapAnalysisService;
        this.roadmapService = roadmapService;
        this.profileService = profileService;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public DashboardData buildFor(User user) {
        long skillCount = userSkillRepository.countByUserId(user.getId());
        long completedAttempts = attemptRepository.countByUserId(user.getId());

        UserProfile profile = profileService.getOrCreateProfile(user.getId());
        JobRole targetRole = profile.getTargetJobRole();

        AnalysisSession latestSession = gapAnalysisService.latest(user);
        GapAnalysis latestAnalysis = null;
        List<GapAnalysisDetail> topGaps = List.of();
        if (latestSession != null) {
            latestAnalysis = gapAnalysisService.getAnalysisForSession(latestSession.getId());
            topGaps = gapAnalysisService.getDetails(latestAnalysis.getId()).stream()
                    .filter(d -> d.getPriority() != Priority.MET)
                    .sorted(Comparator.comparing((GapAnalysisDetail d) -> d.getPriority().ordinal()).reversed()
                            .thenComparing(GapAnalysisDetail::getGapPercentage, Comparator.reverseOrder()))
                    .limit(5)
                    .toList();
        }

        List<AssessmentAttempt> recentAttempts = attemptRepository.findByUserIdOrderByCompletedAtDesc(user.getId())
                .stream().limit(5).toList();
        List<AnalysisSession> recentSessions = gapAnalysisService.history(user).stream().limit(5).toList();

        LearningRoadmap roadmap = roadmapService.currentRoadmap(user);
        double roadmapProgress = 0.0;
        long taskCount = 0;
        if (roadmap != null) {
            roadmapProgress = roadmapService.progressPercent(roadmap.getId());
            taskCount = taskRepository.countByRoadmapId(roadmap.getId());
        }

        boolean hasAnyData = skillCount > 0 || completedAttempts > 0 || latestSession != null || taskCount > 0;
        return new DashboardData(skillCount, completedAttempts, taskCount, targetRole, latestSession,
                latestAnalysis, topGaps, recentAttempts, recentSessions, roadmapProgress, hasAnyData);
    }
}
