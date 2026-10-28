package com.skillgap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Add/edit a skill in "My Skills". */
public class UserSkillForm {

    /** Chosen from the catalog; null/0 when typing a new skill name. */
    private Long skillId;

    /** Used only when no catalog skill was chosen. */
    private String newSkillName;

    @NotNull(message = "Proficiency level is required")
    @Min(value = 1, message = "Level must be between 1 and 5")
    @Max(value = 5, message = "Level must be between 1 and 5")
    private Integer level;

    @Min(value = 0, message = "Years cannot be negative")
    @Max(value = 60, message = "Years look unrealistic")
    private Double yearsOfExperience;

    private String notes;

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getNewSkillName() {
        return newSkillName;
    }

    public void setNewSkillName(String newSkillName) {
        this.newSkillName = newSkillName;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Double getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Double yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
