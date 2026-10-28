package com.skillgap.entity;

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
import jakarta.persistence.UniqueConstraint;

/** Links a job role to a required skill with target level, weight and type. */
@Entity
@Table(name = "job_role_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_job_role_skills", columnNames = {"job_role_id", "skill_id"}),
        indexes = @Index(name = "idx_job_role_skills_role", columnList = "job_role_id"))
public class JobRoleSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRole jobRole;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    /** Required proficiency 1..5. */
    @Column(nullable = false)
    private Integer requiredLevel;

    /** Importance weight 1 (minor) .. 5 (critical). */
    @Column(nullable = false)
    private Integer weight = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequirementType requirementType = RequirementType.REQUIRED;

    protected JobRoleSkill() {
    }

    public JobRoleSkill(Skill skill, Integer requiredLevel, Integer weight, RequirementType requirementType) {
        this.skill = skill;
        this.requiredLevel = requiredLevel;
        this.weight = weight;
        this.requirementType = requirementType;
    }

    public JobRole getJobRole() {
        return jobRole;
    }

    public void setJobRole(JobRole jobRole) {
        this.jobRole = jobRole;
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
}
