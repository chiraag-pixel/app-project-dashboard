package com.skillgap.service;

import com.skillgap.dto.RegisterForm;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.entity.enums.Role;
import com.skillgap.exception.BusinessException;
import com.skillgap.repository.UserRepository;
import com.skillgap.repository.UserProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles new user registration. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, UserProfileRepository userProfileRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterForm form) {
        String email = form.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("An account with this email already exists.");
        }
        // Only the password hash is stored, never the plain password.
        User user = new User(form.getFullName().trim(), email, passwordEncoder.encode(form.getPassword()), Role.USER);
        userRepository.save(user);
        userProfileRepository.save(new UserProfile(user));
        return user;
    }
}
