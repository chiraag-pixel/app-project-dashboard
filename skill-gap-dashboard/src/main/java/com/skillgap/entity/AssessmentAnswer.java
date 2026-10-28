package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** The option a user selected for one question in an attempt. */
@Entity
@Table(name = "assessment_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_answers_attempt_question", columnNames = {"assessment_attempt_id", "assessment_question_id"}),
        indexes = @Index(name = "idx_answers_attempt", columnList = "assessment_attempt_id"))
public class AssessmentAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_attempt_id", nullable = false)
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assessment_question_id", nullable = false)
    private AssessmentQuestion question;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assessment_option_id")
    private AssessmentOption selectedOption;

    @Column(nullable = false)
    private boolean correct;

    protected AssessmentAnswer() {
    }

    public AssessmentAnswer(AssessmentAttempt attempt, AssessmentQuestion question, AssessmentOption selectedOption, boolean correct) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = correct;
    }

    public AssessmentAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(AssessmentAttempt attempt) {
        this.attempt = attempt;
    }

    public AssessmentQuestion getQuestion() {
        return question;
    }

    public void setQuestion(AssessmentQuestion question) {
        this.question = question;
    }

    public AssessmentOption getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(AssessmentOption selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
