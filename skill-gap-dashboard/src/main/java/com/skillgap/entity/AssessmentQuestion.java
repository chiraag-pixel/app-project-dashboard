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

/** One multiple-choice question inside an assessment. */
@Entity
@Table(name = "assessment_questions", indexes = @Index(name = "idx_questions_assessment", columnList = "assessment_id"))
public class AssessmentQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false, length = 1000)
    private String questionText;

    @Column(nullable = false)
    private Integer sortOrder = 1;

    @OneToMany(mappedBy = "question", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AssessmentOption> options = new ArrayList<>();

    protected AssessmentQuestion() {
    }

    public AssessmentQuestion(Assessment assessment, String questionText, Integer sortOrder) {
        this.assessment = assessment;
        this.questionText = questionText;
        this.sortOrder = sortOrder;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<AssessmentOption> getOptions() {
        return options;
    }

    public void setOptions(List<AssessmentOption> options) {
        this.options = options;
    }

    public void addOption(AssessmentOption option) {
        option.setQuestion(this);
        options.add(option);
    }
}
