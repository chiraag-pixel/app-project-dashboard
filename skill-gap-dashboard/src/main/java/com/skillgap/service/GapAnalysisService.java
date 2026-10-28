package com.skillgap.service;

import com.skillgap.entity.AnalysisRequirement;
import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.GapAnalysis;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.InputDocument;
import com.skillgap.entity.JobRole;
import com.skillgap.entity.JobRoleSkill;
import com.skillgap.entity.LearningResource;
import com.skillgap.entity.Skill;
import com.skillgap.entity.User;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.Priority;
import com.skillgap.entity.enums.RequirementType;
import com.skillgap.entity.enums.TargetType;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.AnalysisRequirementRepository;
import com.skillgap.repository.AnalysisSessionRepository;
import com.skillgap.repository.GapAnalysisDetailRepository;
import com.skillgap.repository.GapAnalysisRepository;
import com.skillgap.repository.JobRoleRepository;
import com.skillgap.repository.LearningResourceRepository;
import com.skillgap.repository.UserSkillRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The skill gap engine. All numbers shown in the UI are computed here from
 * database data - nothing is hardcoded.
 *
 * Formulas (from the project specification):
 *   Gap            = max(0, Required Level - Current Level)
 *   Gap Percentage = Gap / Required Level * 100
 *   Alignment      = 100 * sum(weight * min(current, required) / required) / sum(weights)
 *                    calculated over REQUIRED skills; unassessed skills count as level 0.
 *
 * Priority rules (deterministic, gap == 0 is MET):
 *   score = weight * gap
 *   CRITICAL: gap >= 3 and weight >= 4, or score >= 12
 *   HIGH:     score >= 6
 *   MEDIUM:   score >= 3
 *   LOW:      everything else
 */
@Service
public class GapAnalysisService {

    private final JobRoleRepository jobRoleRepository;
    private final UserSkillRepository userSkillRepository;
    private final AnalysisSessionRepository sessionRepository;
    private final GapAnalysisRepository gapAnalysisRepository;
    private final GapAnalysisDetailRepository detailRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final AnalysisRequirementRepository requirementRepository;

    public GapAnalysisService(JobRoleRepository jobRoleRepository,
                              UserSkillRepository userSkillRepository,
                              AnalysisSessionRepository sessionRepository,
                              GapAnalysisRepository gapAnalysisRepository,
                              GapAnalysisDetailRepository detailRepository,
                              LearningResourceRepository learningResourceRepository,
                              AnalysisRequirementRepository requirementRepository) {
        this.jobRoleRepository = jobRoleRepository;
        this.userSkillRepository = userSkillRepository;
        this.sessionRepository = sessionRepository;
        this.gapAnalysisRepository = gapAnalysisRepository;
        this.detailRepository = detailRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.requirementRepository = requirementRepository;
    }

    // ----------------------------------------------------------------- targets

    /** Runs and saves an analysis of the user against a predefined job role. */
    @Transactional
    public AnalysisSession analyzeJobRole(User user, Long jobRoleId) {
        JobRole role = jobRoleRepository.findById(jobRoleId)
                .orElseThrow(() -> new NotFoundException("Job role not found"));
        List<Requirement> requirements = role.getSkills().stream()
                .map(s -> new Requirement(s.getSkill(), s.getRequiredLevel(), s.getWeight(), s.getRequirementType()))
                .toList();
        if (requirements.isEmpty()) {
            throw new BusinessException("This job role has no configured skills yet.");
        }
        return computeAndSave(user, role.getName(), TargetType.JOB_ROLE, role, null, requirements);
    }

    /** Runs and saves an analysis against the confirmed requirements of a document. */
    @Transactional
    public AnalysisSession analyzeDocument(User user, InputDocument document) {
        List<AnalysisRequirement> confirmed = requirementRepository
                .findByUserIdAndInputDocumentIdAndVerificationStatus(user.getId(), document.getId(),
                        com.skillgap.entity.enums.VerificationStatus.CONFIRMED);
        return analyzeRequirements(user, document, confirmed);
    }

    @Transactional
    public AnalysisSession analyzeRequirements(User user, InputDocument document,
                                               List<AnalysisRequirement> confirmed) {
        if (confirmed.isEmpty()) {
            throw new BusinessException("No confirmed requirements yet. Review and confirm at least one skill "
                    + "requirement before running the analysis.");
        }
        List<Requirement> requirements = confirmed.stream()
                .map(r -> new Requirement(r.getSkill(), r.getRequiredLevel(), r.getWeight(), r.getRequirementType()))
                .toList();
        String label = document.getTitle() == null ? "Custom requirements" : document.getTitle();
        TargetType type = document.getDocumentType() == com.skillgap.entity.enums.DocumentType.JOB_DESCRIPTION
                ? TargetType.CUSTOM : TargetType.WORK_SPECIFICATION;
        return computeAndSave(user, label, type, null, document, requirements);
    }

    // ---------------------------------------------------------------- calculation

    @Transactional
    public AnalysisSession computeAndSave(User user, String targetLabel, TargetType targetType,
                                          JobRole jobRole, InputDocument document, List<Requirement> requirements) {
        Map<Long, Integer> currentLevels = new HashMap<>();
        for (UserSkill us : userSkillRepository.findWithSkillByUserId(user.getId())) {
            currentLevels.put(us.getSkill().getId(), us.getLevel());
        }

        AnalysisSession session = new AnalysisSession(user, targetLabel, targetType);
        session.setJobRole(jobRole);
        session.setInputDocument(document);
        session.setAnalyzedAt(Instant.now());

        GapAnalysis gapAnalysis = new GapAnalysis(session);

        List<GapAnalysisDetail> details = new ArrayList<>();
        double alignmentNumerator = 0.0;
        double totalRequiredWeight = 0.0;
        int meets = 0;
        int partial = 0;
        int unassessed = 0;
        int critical = 0;

        List<Requirement> ordered = new ArrayList<>(requirements);
        ordered.sort(Comparator.comparing((Requirement r) -> r.skill().getName()));

        for (Requirement requirement : ordered) {
            Integer current = currentLevels.get(requirement.skill().getId());
            boolean assessed = current != null;
            int effectiveCurrent = assessed ? current : 0;
            int gap = Math.max(0, requirement.requiredLevel() - effectiveCurrent);
            double gapPercentage = gap / (double) requirement.requiredLevel() * 100.0;

            GapAnalysisDetail detail = new GapAnalysisDetail(gapAnalysis, requirement.skill());
            detail.setRequiredLevel(requirement.requiredLevel());
            detail.setCurrentLevel(current);
            detail.setAssessed(assessed);
            detail.setGap(gap);
            detail.setGapPercentage(Math.round(gapPercentage * 10.0) / 10.0);
            detail.setWeight(requirement.weight());
            detail.setRequirementType(requirement.type());
            detail.setPriority(priorityFor(gap, requirement.weight()));
            applyRecommendation(detail);
            details.add(detail);

            // Alignment only considers mandatory (REQUIRED) skills.
            if (requirement.type() == RequirementType.REQUIRED) {
                totalRequiredWeight += requirement.weight();
                alignmentNumerator += requirement.weight()
                        * (Math.min(effectiveCurrent, requirement.requiredLevel()) / (double) requirement.requiredLevel());
            }

            if (gap == 0) {
                meets++;
            } else {
                partial++;
                if (detail.getPriority() == Priority.CRITICAL) {
                    critical++;
                }
            }
            if (!assessed) {
                unassessed++;
            }
        }

        double alignment = totalRequiredWeight == 0 ? 0.0
                : Math.round(100.0 * alignmentNumerator / totalRequiredWeight * 10.0) / 10.0;

        session.setAlignmentPercentage(alignment);
        session.setRequiredSkillCount((int) ordered.stream().filter(r -> r.type() == RequirementType.REQUIRED).count());
        session.setCriticalGapCount(critical);
        sessionRepository.save(session);

        gapAnalysis.setAlignmentPercentage(alignment);
        gapAnalysis.setTotalWeight(totalRequiredWeight);
        gapAnalysis.setMeetsCount(meets);
        gapAnalysis.setPartialCount(partial - unassessed);
        gapAnalysis.setUnassessedCount(unassessed);
        gapAnalysis.setCriticalCount(critical);
        gapAnalysis.setNotes(buildNotes(unassessed));
        gapAnalysis.setCalculatedAt(Instant.now());
        gapAnalysisRepository.save(gapAnalysis);

        detailRepository.saveAll(details);
        return session;
    }

    /** Priority rule from the specification: deterministic, understandable. */
    public Priority priorityFor(int gap, int weight) {
        if (gap <= 0) {
            return Priority.MET;
        }
        int score = gap * weight;
        if ((gap >= 3 && weight >= 4) || score >= 12) {
            return Priority.CRITICAL;
        }
        if (score >= 6) {
            return Priority.HIGH;
        }
        if (score >= 3) {
            return Priority.MEDIUM;
        }
        return Priority.LOW;
    }

    private void applyRecommendation(GapAnalysisDetail detail) {
        if (detail.getPriority() == Priority.MET) {
            detail.setRecommendation("Skill target met - keep it current.");
            return;
        }
        List<LearningResource> resources = learningResourceRepository
                .findByActiveTrueAndSkillId(detail.getSkill().getId());
        if (!resources.isEmpty()) {
            LearningResource top = resources.get(0);
            detail.setRecommendation("Study: " + top.getTitle()
                    + (top.getEstimatedHours() != null ? " (about " + trim(top.getEstimatedHours()) + " hours)" : ""));
            detail.setRecommendationUrl(top.getUrl());
        } else {
            detail.setRecommendation("Practice " + detail.getSkill().getName()
                    + " until you reach level " + detail.getRequiredLevel() + ".");
        }
    }

    private String buildNotes(int unassessed) {
        if (unassessed == 0) {
            return "All required skills had a recorded level, so the alignment reflects your confirmed profile.";
        }
        return unassessed + " skill(s) have not been assessed yet. They are treated as level 0 for this "
                + "calculation, which lowers the alignment score. Record or assess these skills to get an "
                + "accurate result - unassessed does NOT mean you have no ability in them.";
    }

    private double trim(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // ----------------------------------------------------------------- retrieval

    @Transactional(readOnly = true)
    public AnalysisSession getSessionOwned(User user, Long sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new NotFoundException("Analysis not found"));
    }

    @Transactional(readOnly = true)
    public GapAnalysis getAnalysisForSession(Long sessionId) {
        return gapAnalysisRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("Analysis result not found"));
    }

    @Transactional(readOnly = true)
    public List<GapAnalysisDetail> getDetails(Long gapAnalysisId) {
        return detailRepository.findByGapAnalysisIdOrderByGapDesc(gapAnalysisId);
    }

    @Transactional(readOnly = true)
    public List<AnalysisSession> history(User user) {
        return sessionRepository.findByUserIdOrderByAnalyzedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public AnalysisSession latest(User user) {
        return sessionRepository.findTopByUserIdOrderByAnalyzedAtDesc(user.getId()).orElse(null);
    }

    /** A requirement row feeding the engine. */
    public record Requirement(Skill skill, int requiredLevel, int weight, RequirementType type) {
    }
}
