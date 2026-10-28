package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Computed summary of one analysis session. */
@Entity
@Table(name = "gap_analyses")
public class GapAnalysis extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_session_id", nullable = false, unique = true)
    private AnalysisSession session;

    /** Alignment = 100 * sum(w * min(cur, req) / req) / sum(w) over REQUIRED skills. */
    @Column(nullable = false)
    private Double alignmentPercentage;

    /** Sum of all required-skill weights used in the alignment calculation. */
    @Column(nullable = false)
    private Double totalWeight;

    @Column(nullable = false)
    private int meetsCount;

    @Column(nullable = false)
    private int partialCount;

    /** Skills the user has not recorded at all (unassessed). */
    @Column(nullable = false)
    private int unassessedCount;

    @Column(nullable = false)
    private int criticalCount;

    /** Explains how unassessed skills were treated in this calculation. */
    @Column(length = 1000)
    private String notes;

    private Instant calculatedAt;

    protected GapAnalysis() {
    }

    public GapAnalysis(AnalysisSession session) {
        this.session = session;
    }

    public AnalysisSession getSession() {
        return session;
    }

    public void setSession(AnalysisSession session) {
        this.session = session;
    }

    public Double getAlignmentPercentage() {
        return alignmentPercentage;
    }

    public void setAlignmentPercentage(Double alignmentPercentage) {
        this.alignmentPercentage = alignmentPercentage;
    }

    public Double getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(Double totalWeight) {
        this.totalWeight = totalWeight;
    }

    public int getMeetsCount() {
        return meetsCount;
    }

    public void setMeetsCount(int meetsCount) {
        this.meetsCount = meetsCount;
    }

    public int getPartialCount() {
        return partialCount;
    }

    public void setPartialCount(int partialCount) {
        this.partialCount = partialCount;
    }

    public int getUnassessedCount() {
        return unassessedCount;
    }

    public void setUnassessedCount(int unassessedCount) {
        this.unassessedCount = unassessedCount;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
