package com.skillgap.entity;

import com.skillgap.entity.enums.ExtractionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One run of the rule-based skill extraction engine over a document or typed text. */
@Entity
@Table(name = "skill_extraction_runs", indexes = @Index(name = "idx_extraction_runs_user", columnList = "user_id"))
public class SkillExtractionRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The analyzed document; may be null when text was typed directly. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_document_id")
    private InputDocument inputDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtractionStatus status = ExtractionStatus.PROCESSING;

    @Column(nullable = false)
    private int candidateCount;

    @Column(nullable = false)
    private int confirmedCount;

    @Column(nullable = false)
    private int rejectedCount;

    /** Human readable note, e.g. failure reason or "no catalog matches found". */
    @Column(length = 500)
    private String message;

    private Instant ranAt;

    protected SkillExtractionRun() {
    }

    public SkillExtractionRun(User user, InputDocument inputDocument) {
        this.user = user;
        this.inputDocument = inputDocument;
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

    public ExtractionStatus getStatus() {
        return status;
    }

    public void setStatus(ExtractionStatus status) {
        this.status = status;
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(int confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getRanAt() {
        return ranAt;
    }

    public void setRanAt(Instant ranAt) {
        this.ranAt = ranAt;
    }
}
