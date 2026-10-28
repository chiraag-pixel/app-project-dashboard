package com.skillgap.controller;

import com.skillgap.dto.CertificationForm;
import com.skillgap.dto.ProfileForm;
import com.skillgap.dto.ProjectForm;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.JobRoleQueryService;
import com.skillgap.service.PortfolioService;
import com.skillgap.service.ProfileService;
import com.skillgap.service.SkillCatalogService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** User profile plus certifications and portfolio projects. */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final ProfileService profileService;
    private final PortfolioService portfolioService;
    private final JobRoleQueryService jobRoleQueryService;
    private final SkillCatalogService skillCatalogService;

    public ProfileController(CurrentUserService currentUserService,
                             ProfileService profileService,
                             PortfolioService portfolioService,
                             JobRoleQueryService jobRoleQueryService,
                             SkillCatalogService skillCatalogService) {
        this.currentUserService = currentUserService;
        this.profileService = profileService;
        this.portfolioService = portfolioService;
        this.jobRoleQueryService = jobRoleQueryService;
        this.skillCatalogService = skillCatalogService;
    }

    @GetMapping
    public String profile(Model model) {
        User user = currentUserService.getUser();
        UserProfile profile = profileService.getOrCreateProfile(user.getId());

        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(user.getFullName());
            form.setEducation(profile.getEducation());
            form.setExperience(profile.getExperience());
            form.setCareerInterests(profile.getCareerInterests());
            form.setTargetJobRoleId(profile.getTargetJobRole() == null ? null : profile.getTargetJobRole().getId());
            model.addAttribute("profileForm", form);
        }
        if (!model.containsAttribute("certificationForm")) {
            model.addAttribute("certificationForm", new CertificationForm());
        }
        if (!model.containsAttribute("projectForm")) {
            model.addAttribute("projectForm", new ProjectForm());
        }
        model.addAttribute("jobRoles", jobRoleQueryService.listRoles());
        model.addAttribute("catalogSkills", skillCatalogService.listSkills());
        model.addAttribute("certifications", portfolioService.listCertifications(user));
        model.addAttribute("projects", portfolioService.listProjects(user));
        model.addAttribute("profile", profile);
        return "profile";
    }

    @PostMapping("/update")
    public String update(@Valid @ModelAttribute("profileForm") ProfileForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            profile(model);
            return "profile";
        }
        profileService.updateProfile(currentUserService.getUser(), form);
        redirectAttributes.addFlashAttribute("message", "Profile updated.");
        return "redirect:/profile";
    }

    @PostMapping("/certifications/add")
    public String addCertification(@Valid @ModelAttribute("certificationForm") CertificationForm form,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        if (bindingResult.hasErrors()) {
            profile(model);
            return "profile";
        }
        try {
            portfolioService.addCertification(user, form);
            redirectAttributes.addFlashAttribute("message", "Certification added as supporting evidence.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/certifications/{id}/delete")
    public String deleteCertification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        portfolioService.deleteCertification(currentUserService.getUser(), id);
        redirectAttributes.addFlashAttribute("message", "Certification removed.");
        return "redirect:/profile";
    }

    @PostMapping("/projects/add")
    public String addProject(@Valid @ModelAttribute("projectForm") ProjectForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        if (bindingResult.hasErrors()) {
            profile(model);
            return "profile";
        }
        try {
            portfolioService.addProject(user, form);
            redirectAttributes.addFlashAttribute("message", "Project added. Projects are treated as supporting evidence.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        portfolioService.deleteProject(currentUserService.getUser(), id);
        redirectAttributes.addFlashAttribute("message", "Project removed.");
        return "redirect:/profile";
    }
}
