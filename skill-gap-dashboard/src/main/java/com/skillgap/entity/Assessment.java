package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** A skill quiz: a title, one target skill and a set of multiple-choice questions. */
@Entity
@Table(name = "assessments", indexes = @Index(name = "idx_assessments_skill", columnList = "skill_id"))
public class Assessment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    /** The skill this quiz measures. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    /** Minimum score (percent) considered a pass. */
    @Column(nullable = false)
    private Integer passingScore = 60;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "assessment", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AssessmentQuestion> questions = new ArrayList<>();

    protected Assessment() {
    }

    public Assessment(String title, Skill skill, Integer passingScore) {
        this.title = title;
        this.skill = skill;
        this.passingScore = passingScore;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public Integer getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(Integer passingScore) {
        this.passingScore = passingScore;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<AssessmentQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<AssessmentQuestion> questions) {
        this.questions = questions;
    }
}
