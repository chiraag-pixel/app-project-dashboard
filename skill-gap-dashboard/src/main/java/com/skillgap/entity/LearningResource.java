package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An admin-managed learning resource (course, book, tutorial) linked to a skill. */
@Entity
@Table(name = "learning_resources", indexes = @Index(name = "idx_resources_skill", columnList = "skill_id"))
public class LearningResource extends BaseEntity {

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    /** Difficulty 1 (beginner) .. 5 (expert). */
    @Column(nullable = false)
    private Integer difficulty = 1;

    /** Estimated effort in hours. */
    @Column(name = "estimated_hours")
    private Double estimatedHours;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected LearningResource() {
    }

    public LearningResource(String title, String url, Skill skill) {
        this.title = title;
        this.url = url;
        this.skill = skill;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
