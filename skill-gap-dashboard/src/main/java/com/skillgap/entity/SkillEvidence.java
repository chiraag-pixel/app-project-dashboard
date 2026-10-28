package com.skillgap.entity;

import com.skillgap.entity.enums.SourceType;
import com.skillgap.entity.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A detected (or manually created) piece of evidence for a skill: where it was
 * found, the surrounding text, an extraction confidence and a verification status.
 */
@Entity
@Table(name = "skill_evidence", indexes = @Index(name = "idx_skill_evidence_user", columnList = "user_id"))
public class SkillEvidence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_run_id")
    private SkillExtractionRun extractionRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType sourceType;

    /** The sentence or fragment where the skill was detected. */
    @Column(length = 2000)
    private String contextSnippet;

    /** 0.0 - 1.0, produced by the rule-based matcher. */
    @Column(nullable = false)
    private Double confidence = 0.5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /** The user skill created after the user confirmed this evidence (if any). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_skill_id")
    private UserSkill userSkill;

    /** Level the user chose while confirming (1..5), kept as evidence. */
    private Integer confirmedLevel;

    private Instant recordedAt;

    protected SkillEvidence() {
    }

    public SkillEvidence(User user, Skill skill, SourceType sourceType) {
        this.user = user;
        this.skill = skill;
        this.sourceType = sourceType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public SkillExtractionRun getExtractionRun() {
        return extractionRun;
    }

    public void setExtractionRun(SkillExtractionRun extractionRun) {
        this.extractionRun = extractionRun;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getContextSnippet() {
        return contextSnippet;
    }

    public void setContextSnippet(String contextSnippet) {
        this.contextSnippet = contextSnippet;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public UserSkill getUserSkill() {
        return userSkill;
    }

    public void setUserSkill(UserSkill userSkill) {
        this.userSkill = userSkill;
    }

    public Integer getConfirmedLevel() {
        return confirmedLevel;
    }

    public void setConfirmedLevel(Integer confirmedLevel) {
        this.confirmedLevel = confirmedLevel;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
