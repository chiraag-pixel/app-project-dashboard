package com.skillgap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.entity.enums.Role;
import com.skillgap.repository.UserProfileRepository;
import com.skillgap.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Registration, login, and server-side authorization (USER vs ADMIN). */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndSecurityTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private com.skillgap.repository.JobRoleRepository jobRoleRepository;

    private String tag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private User createUser(Role role, String rawPassword) {
        String tag = tag();
        User user = new User("Test User " + tag, "user-" + tag + "@test.com",
                passwordEncoder.encode(rawPassword), role);
        userRepository.save(user);
        userProfileRepository.save(new UserProfile(user));
        return user;
    }

    // ------------------------------------------------------------------- public

    @Test
    void loginAndRegistrationPagesArePublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("auth/register"));
    }

    @Test
    void allProtectedPagesRedirectAnonymousUsersToLogin() throws Exception {
        List<String> pages = List.of("/dashboard", "/skills", "/documents", "/job-roles",
                "/work-specs", "/analysis", "/assessments", "/roadmap", "/reports", "/profile");
        for (String page : pages) {
            mvc.perform(get(page))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    // ------------------------------------------------------------ registration

    @Test
    void registrationStoresOnlyABcryptHash() throws Exception {
        String email = "register-" + tag() + "@test.com";
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Registered User")
                        .param("email", email)
                        .param("password", "Secret123!")
                        .param("confirmPassword", "Secret123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("Secret123!");
        assertThat(saved.getPassword()).startsWith("$2");           // BCrypt hash
        assertThat(passwordEncoder.matches("Secret123!", saved.getPassword())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(userProfileRepository.findByUserId(saved.getId())).isPresent();
    }

    @Test
    void duplicateRegistrationShowsFriendlyErrorInsteadOfStackTrace() throws Exception {
        User existing = createUser(Role.USER, "Password123!");
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Copycat")
                        .param("email", existing.getEmail())
                        .param("password", "Another123!")
                        .param("confirmPassword", "Another123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void registrationValidationRejectsMismatchedPasswords() throws Exception {
        String email = "invalid-" + tag() + "@test.com";
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Invalid User")
                        .param("email", email)
                        .param("password", "Secret123!")
                        .param("confirmPassword", "Different123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    // ------------------------------------------------------------------- login

    @Test
    void userCanLogInWithCorrectPasswordAndIsSentToDashboard() throws Exception {
        User user = createUser(Role.USER, "Password123!");
        mvc.perform(post("/login").with(csrf())
                        .param("username", user.getEmail())
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        User user = createUser(Role.USER, "Password123!");
        mvc.perform(post("/login").with(csrf())
                        .param("username", user.getEmail())
                        .param("password", "totally-wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    // -------------------------------------------------------- role restrictions

    @Test
    void adminPanelIsBlockedForRegularUsersOnServerSide() throws Exception {
        User regular = createUser(Role.USER, "Password123!");

        mvc.perform(get("/admin").with(user(regular.getEmail())))
                .andExpect(status().isForbidden());

        // The POST is rejected too, even though a browser could craft it.
        mvc.perform(post("/admin/categories/add").with(user(regular.getEmail())).with(csrf())
                        .param("name", "Sneaky Category"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPanelIsOpenToAdministrators() throws Exception {
        User admin = createUser(Role.ADMIN, "Password123!");
        mvc.perform(get("/admin").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/index"));
        mvc.perform(get("/admin/users").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/skills").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/roles").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/assessments").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/resources").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/categories").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());

        // Role detail page (requirements editor) renders for admins too.
        var role = jobRoleRepository.save(new com.skillgap.entity.JobRole("Admin Role " + tag(), "for tests"));
        mvc.perform(get("/admin/roles/" + role.getId()).with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/role-detail"));
    }
}
