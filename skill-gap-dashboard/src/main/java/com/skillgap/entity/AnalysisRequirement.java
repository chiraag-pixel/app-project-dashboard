package com.skillgap.entity;

import com.skillgap.entity.enums.RequirementType;
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
import jakarta.persistence.UniqueConstraint;

/**
 * A single skill requirement extracted from a work specification or custom job
 * description. Users review these rows (confirm / edit / reject) before running
 * an analysis against them.
 */
@Entity
@Table(name = "analysis_requirements",
        uniqueConstraints = @UniqueConstraint(name = "uk_analysis_requirements",
                columnNames = {"user_id", "input_document_id", "skill_id"}),
        indexes = @Index(name = "idx_analysis_requirements_doc", columnList = "input_document_id"))
public class AnalysisRequirement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The work specification or custom job description this requirement came from. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_document_id", nullable = false)
    private InputDocument inputDocument;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    /** Target proficiency 1..5 (default 3 when the extractor could not infer one). */
    @Column(nullable = false)
    private Integer requiredLevel;

    /** Importance weight 1..5. */
    @Column(nullable = false)
    private Integer weight = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequirementType requirementType = RequirementType.REQUIRED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /** The sentence in the document that triggered the match. */
    @Column(length = 2000)
    private String contextSnippet;

    protected AnalysisRequirement() {
    }

    public AnalysisRequirement(User user, InputDocument inputDocument, Skill skill, Integer requiredLevel) {
        this.user = user;
        this.inputDocument = inputDocument;
        this.skill = skill;
        this.requiredLevel = requiredLevel;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public InputDocument getInputDocument() {
        return inputDocument;
    }

    public void setInputDocument(InputDocument inputDocument) {
        this.inputDocument = inputDocument;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public Integer getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(Integer requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public RequirementType getRequirementType() {
        return requirementType;
    }

    public void setRequirementType(RequirementType requirementType) {
        this.requirementType = requirementType;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getContextSnippet() {
        return contextSnippet;
    }

    public void setContextSnippet(String contextSnippet) {
        this.contextSnippet = contextSnippet;
    }
}
