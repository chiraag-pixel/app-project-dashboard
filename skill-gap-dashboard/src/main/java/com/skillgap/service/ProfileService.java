package com.skillgap.service;

import com.skillgap.dto.ProfileForm;
import com.skillgap.entity.JobRole;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.repository.JobRoleRepository;
import com.skillgap.repository.UserProfileRepository;
import com.skillgap.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and updates the current user's profile. */
@Service
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final JobRoleRepository jobRoleRepository;

    public ProfileService(UserProfileRepository userProfileRepository, UserRepository userRepository,
                          JobRoleRepository jobRoleRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.jobRoleRepository = jobRoleRepository;
    }

    @Transactional
    public UserProfile getOrCreateProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    return userProfileRepository.save(new UserProfile(user));
                });
    }

    @Transactional
    public void updateProfile(User user, ProfileForm form) {
        user.setFullName(form.getFullName().trim());
        userRepository.save(user);

        UserProfile profile = getOrCreateProfile(user.getId());
        profile.setEducation(trimToNull(form.getEducation()));
        profile.setExperience(trimToNull(form.getExperience()));
        profile.setCareerInterests(trimToNull(form.getCareerInterests()));
        if (form.getTargetJobRoleId() == null || form.getTargetJobRoleId() <= 0) {
            profile.setTargetJobRole(null);
        } else {
            JobRole role = jobRoleRepository.findById(form.getTargetJobRoleId()).orElse(null);
            profile.setTargetJobRole(role);
        }
        userProfileRepository.save(profile);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
