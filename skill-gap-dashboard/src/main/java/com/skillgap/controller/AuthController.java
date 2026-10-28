package com.skillgap.controller;

import com.skillgap.dto.RegisterForm;
import com.skillgap.entity.User;
import com.skillgap.security.LoginThrottleService;
import com.skillgap.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Public pages: login and registration. */
@Controller
public class AuthController {

    private final AuthService authService;
    private final LoginThrottleService loginThrottleService;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthService authService, LoginThrottleService loginThrottleService,
                          UserDetailsService userDetailsService) {
        this.authService = authService;
        this.loginThrottleService = loginThrottleService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage",
                    "Invalid email or password. Too many failed attempts lock login temporarily.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            User user = authService.register(form);
            // Sign the new user in immediately for a smooth first run.
            UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
            redirectAttributes.addFlashAttribute("message", "Welcome, " + user.getFullName() + "! Your account is ready.");
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
}
