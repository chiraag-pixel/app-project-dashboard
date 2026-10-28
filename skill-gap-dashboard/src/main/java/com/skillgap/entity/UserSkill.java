package com.skillgap.entity;

import com.skillgap.entity.enums.SourceType;
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
import java.time.Instant;

/**
 * A skill recorded by a user with a proficiency level (1..5).
 * A skill the user never recorded is "unassessed" - there is no row, which is
 * deliberately different from a rated level of 1 (Beginner).
 */
@Entity
@Table(name = "user_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_skills_user_skill", columnNames = {"user_id", "skill_id"}),
        indexes = @Index(name = "idx_user_skills_user", columnList = "user_id"))
public class UserSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    /** 1 = Beginner, 2 = Basic, 3 = Intermediate, 4 = Advanced, 5 = Expert. */
    @Column(nullable = false)
    private Integer level;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    /** Short evidence / notes, e.g. "Built a REST API in a college project". */
    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType sourceType = SourceType.MANUAL;

    private Instant lastAssessedAt;

    protected UserSkill() {
    }

    public UserSkill(User user, Skill skill, Integer level) {
        this.user = user;
        this.skill = skill;
        this.level = level;
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

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Double getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Double yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Instant getLastAssessedAt() {
        return lastAssessedAt;
    }

    public void setLastAssessedAt(Instant lastAssessedAt) {
        this.lastAssessedAt = lastAssessedAt;
    }
}
