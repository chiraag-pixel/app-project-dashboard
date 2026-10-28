package com.skillgap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Profile edit form. */
public class ProfileForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 120)
    private String fullName;

    @Size(max = 200)
    private String education;

    @Size(max = 2000)
    private String experience;

    @Size(max = 1000)
    private String careerInterests;

    private Long targetJobRoleId;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getCareerInterests() {
        return careerInterests;
    }

    public void setCareerInterests(String careerInterests) {
        this.careerInterests = careerInterests;
    }

    public Long getTargetJobRoleId() {
        return targetJobRoleId;
    }

    public void setTargetJobRoleId(Long targetJobRoleId) {
        this.targetJobRoleId = targetJobRoleId;
    }
}
