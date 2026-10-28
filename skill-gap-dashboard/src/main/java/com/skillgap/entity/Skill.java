package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A skill in the shared catalog, e.g. "Java", "Spring Boot", "SQL". */
@Entity
@Table(name = "skills", indexes = @Index(name = "idx_skills_category", columnList = "skill_category_id"))
public class Skill extends BaseEntity {

    /** Display name, e.g. "Spring Boot". */
    @Column(nullable = false, length = 120)
    private String name;

    /** Lower-case, trimmed form used for matching and uniqueness. */
    @Column(nullable = false, unique = true, length = 140)
    private String normalizedName;

    @Column(length = 1000)
    private String description;

    /** Comma separated extra terms used by the extraction engine, e.g. "springboot, spring mvc". */
    @Column(length = 1000)
    private String aliases;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_category_id")
    private SkillCategory category;

    protected Skill() {
    }

    public Skill(String name, String normalizedName, SkillCategory category) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public void setCategory(SkillCategory category) {
        this.category = category;
    }
}
