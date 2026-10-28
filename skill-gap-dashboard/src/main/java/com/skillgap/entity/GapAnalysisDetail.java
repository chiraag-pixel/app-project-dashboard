package com.skillgap.entity;

import com.skillgap.entity.enums.Priority;
import com.skillgap.entity.enums.RequirementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Per-skill result of a gap analysis. */
@Entity
@Table(name = "gap_analysis_details", indexes = @Index(name = "idx_gap_details_analysis", columnList = "gap_analysis_id"))
public class GapAnalysisDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gap_analysis_id", nullable = false)
    private GapAnalysis gapAnalysis;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private Integer requiredLevel;

    /** Current level, or null when the skill is unassessed. */
    private Integer currentLevel;

    /** False when the user never recorded this skill (distinct from level 1). */
    @Column(nullable = false)
    private boolean assessed;

    /** Gap = max(0, required - current). Unassessed skills use current = 0. */
    @Column(nullable = false)
    private Integer gap;

    /** Gap Percentage = Gap / Required * 100. */
    @Column(nullable = false)
    private Double gapPercentage;

    @Column(nullable = false)
    private Integer weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequirementType requirementType = RequirementType.REQUIRED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    /** Recommended learning resource title / generic advice for this skill. */
    @Column(length = 500)
    private String recommendation;

    /** URL of the recommended resource, when one exists in the catalog. */
    @Column(length = 500)
    private String recommendationUrl;

    protected GapAnalysisDetail() {
    }

    public GapAnalysisDetail(GapAnalysis gapAnalysis, Skill skill) {
        this.gapAnalysis = gapAnalysis;
        this.skill = skill;
    }

    public GapAnalysis getGapAnalysis() {
        return gapAnalysis;
    }

    public void setGapAnalysis(GapAnalysis gapAnalysis) {
        this.gapAnalysis = gapAnalysis;
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

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public boolean isAssessed() {
        return assessed;
    }

    public void setAssessed(boolean assessed) {
        this.assessed = assessed;
    }

    public Integer getGap() {
        return gap;
    }

    public void setGap(Integer gap) {
        this.gap = gap;
    }

    public Double getGapPercentage() {
        return gapPercentage;
    }

    public void setGapPercentage(Double gapPercentage) {
        this.gapPercentage = gapPercentage;
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

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getRecommendationUrl() {
        return recommendationUrl;
    }

    public void setRecommendationUrl(String recommendationUrl) {
        this.recommendationUrl = recommendationUrl;
    }
}
