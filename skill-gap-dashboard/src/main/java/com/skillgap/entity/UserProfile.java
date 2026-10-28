package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Optional profile details for a user. */
@Entity
@Table(name = "user_profiles", indexes = @Index(name = "idx_user_profiles_target_role", columnList = "target_job_role_id"))
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 200)
    private String education;

    @Column(length = 2000)
    private String experience;

    @Column(length = 1000)
    private String careerInterests;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_job_role_id")
    private JobRole targetJobRole;

    protected UserProfile() {
    }

    public UserProfile(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public JobRole getTargetJobRole() {
        return targetJobRole;
    }

    public void setTargetJobRole(JobRole targetJobRole) {
        this.targetJobRole = targetJobRole;
    }
}
