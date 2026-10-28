package com.skillgap.service;

import com.skillgap.entity.SkillEvidence;
import com.skillgap.entity.User;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.SkillEvidenceRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets users confirm or reject automatically extracted skills. */
@Service
public class EvidenceService {

    private final SkillEvidenceRepository evidenceRepository;
    private final UserSkillService userSkillService;

    public EvidenceService(SkillEvidenceRepository evidenceRepository, UserSkillService userSkillService) {
        this.evidenceRepository = evidenceRepository;
        this.userSkillService = userSkillService;
    }

    public List<SkillEvidence> listForUser(User user, Long runId) {
        return evidenceRepository.findByExtractionRunIdOrderBySkillNameAsc(runId);
    }

    public SkillEvidence getOwned(User user, Long evidenceId) {
        return evidenceRepository.findByIdAndUserId(evidenceId, user.getId())
                .orElseThrow(() -> new NotFoundException("Extracted skill not found"));
    }

    /** User confirms a detected skill at a chosen level: saved to My Skills with evidence. */
    @Transactional
    public UserSkill confirm(User user, Long evidenceId, int level, Double yearsOfExperience) {
        if (level < 1 || level > 5) {
            throw new BusinessException("Level must be between 1 and 5.");
        }
        SkillEvidence evidence = getOwned(user, evidenceId);
        if (evidence.getVerificationStatus() == VerificationStatus.CONFIRMED) {
            throw new BusinessException("This skill was already confirmed.");
        }
        UserSkill userSkill = userSkillService.upsert(user, evidence.getSkill(), level,
                evidence.getSourceType(), "Confirmed from " + label(evidence));
        if (yearsOfExperience != null) {
            userSkill.setYearsOfExperience(yearsOfExperience);
        }
        evidence.setVerificationStatus(VerificationStatus.CONFIRMED);
        evidence.setConfirmedLevel(level);
        evidence.setRecordedAt(Instant.now());
        evidence.setUserSkill(userSkill);
        evidenceRepository.save(evidence);
        return userSkill;
    }

    /** User rejects a false positive; it stays stored as rejected evidence. */
    @Transactional
    public void reject(User user, Long evidenceId) {
        SkillEvidence evidence = getOwned(user, evidenceId);
        if (evidence.getVerificationStatus() == VerificationStatus.CONFIRMED) {
            throw new BusinessException("This skill was already confirmed and saved. Remove it from My Skills instead.");
        }
        evidence.setVerificationStatus(VerificationStatus.REJECTED);
        evidenceRepository.save(evidence);
    }

    private String label(SkillEvidence evidence) {
        return switch (evidence.getSourceType()) {
            case RESUME -> "resume";
            case JOB_DESCRIPTION -> "job description";
            case WORK_SPECIFICATION -> "work specification";
            case PROJECT -> "project";
            case ASSESSMENT -> "assessment";
            case CERTIFICATION -> "certification";
            case MANUAL -> "manual entry";
        };
    }
}
