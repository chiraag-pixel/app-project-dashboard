package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One completed (or in-progress) attempt of an assessment by a user. */
@Entity
@Table(name = "assessment_attempts", indexes = @Index(name = "idx_attempts_user", columnList = "user_id"))
public class AssessmentAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int totalQuestions;

    /** Score 0..100. */
    @Column(nullable = false)
    private Integer scorePercent;

    @Column(nullable = false)
    private boolean passed;

    /** True once the user applied this attempt's score to their skill profile. */
    @Column(nullable = false)
    private boolean appliedToProfile = false;

    private Instant startedAt;

    private Instant completedAt;

    protected AssessmentAttempt() {
    }

    public AssessmentAttempt(User user, Assessment assessment, Instant startedAt) {
        this.user = user;
        this.assessment = assessment;
        this.startedAt = startedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getScorePercent() {
        return scorePercent;
    }

    public void setScorePercent(Integer scorePercent) {
        this.scorePercent = scorePercent;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isAppliedToProfile() {
        return appliedToProfile;
    }

    public void setAppliedToProfile(boolean appliedToProfile) {
        this.appliedToProfile = appliedToProfile;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
