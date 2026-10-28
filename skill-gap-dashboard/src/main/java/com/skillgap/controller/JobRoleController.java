package com.skillgap.controller;

import com.skillgap.entity.User;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.service.JobRoleQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Browse predefined job roles and run an analysis against one. */
@Controller
@RequestMapping("/job-roles")
public class JobRoleController {

    private final CurrentUserService currentUserService;
    private final JobRoleQueryService jobRoleQueryService;
    private final GapAnalysisService gapAnalysisService;

    public JobRoleController(CurrentUserService currentUserService,
                             JobRoleQueryService jobRoleQueryService,
                             GapAnalysisService gapAnalysisService) {
        this.currentUserService = currentUserService;
        this.jobRoleQueryService = jobRoleQueryService;
        this.gapAnalysisService = gapAnalysisService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", jobRoleQueryService.listRoles());
        return "jobroles/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("role", jobRoleQueryService.getRole(id));
        return "jobroles/detail";
    }

    @PostMapping("/{id}/analyze")
    public String analyze(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            var session = gapAnalysisService.analyzeJobRole(user, id);
            return "redirect:/analysis/" + session.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/job-roles/" + id;
        }
    }
}
