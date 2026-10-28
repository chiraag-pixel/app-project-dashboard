package com.skillgap.entity;

import com.skillgap.entity.enums.TargetType;
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
 * One saved skill-gap analysis: which target it measured against and when.
 * Per-skill results live in GapAnalysisDetail via GapAnalysis.
 */
@Entity
@Table(name = "analysis_sessions", indexes = @Index(name = "idx_analysis_sessions_user", columnList = "user_id"))
public class AnalysisSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Label shown in history, e.g. "Java Developer" or the work spec title. */
    @Column(nullable = false, length = 200)
    private String targetLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id")
    private JobRole jobRole;

    /** The work specification / custom job description document, when applicable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_document_id")
    private InputDocument inputDocument;

    /** Weighted role alignment percentage at the time of analysis (0..100). */
    @Column(nullable = false)
    private Double alignmentPercentage;

    @Column(nullable = false)
    private int requiredSkillCount;

    @Column(nullable = false)
    private int criticalGapCount;

    private Instant analyzedAt;

    protected AnalysisSession() {
    }

    public AnalysisSession(User user, String targetLabel, TargetType targetType) {
        this.user = user;
        this.targetLabel = targetLabel;
        this.targetType = targetType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public JobRole getJobRole() {
        return jobRole;
    }

    public void setJobRole(JobRole jobRole) {
        this.jobRole = jobRole;
    }

    public InputDocument getInputDocument() {
        return inputDocument;
    }

    public void setInputDocument(InputDocument inputDocument) {
        this.inputDocument = inputDocument;
    }

    public Double getAlignmentPercentage() {
        return alignmentPercentage;
    }

    public void setAlignmentPercentage(Double alignmentPercentage) {
        this.alignmentPercentage = alignmentPercentage;
    }

    public int getRequiredSkillCount() {
        return requiredSkillCount;
    }

    public void setRequiredSkillCount(int requiredSkillCount) {
        this.requiredSkillCount = requiredSkillCount;
    }

    public int getCriticalGapCount() {
        return criticalGapCount;
    }

    public void setCriticalGapCount(int criticalGapCount) {
        this.criticalGapCount = criticalGapCount;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
