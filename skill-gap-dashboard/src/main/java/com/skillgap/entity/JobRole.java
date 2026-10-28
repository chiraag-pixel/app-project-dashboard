package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** A predefined job role such as "Java Developer" with required skills. */
@Entity
@Table(name = "job_roles")
public class JobRole extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    @OneToMany(mappedBy = "jobRole", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<JobRoleSkill> skills = new ArrayList<>();

    protected JobRole() {
    }

    public JobRole(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<JobRoleSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<JobRoleSkill> skills) {
        this.skills = skills;
    }

    public void addSkill(JobRoleSkill skill) {
        skill.setJobRole(this);
        skills.add(skill);
    }

    public void removeSkills() {
        skills.clear();
    }
}
