package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An answer option of a multiple-choice question. */
@Entity
@Table(name = "assessment_options", indexes = @Index(name = "idx_options_question", columnList = "question_id"))
public class AssessmentOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(nullable = false, length = 500)
    private String optionText;

    @Column(nullable = false)
    private boolean correct = false;

    protected AssessmentOption() {
    }

    public AssessmentOption(AssessmentQuestion question, String optionText, boolean correct) {
        this.question = question;
        this.optionText = optionText;
        this.correct = correct;
    }

    public AssessmentQuestion getQuestion() {
        return question;
    }

    public void setQuestion(AssessmentQuestion question) {
        this.question = question;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
