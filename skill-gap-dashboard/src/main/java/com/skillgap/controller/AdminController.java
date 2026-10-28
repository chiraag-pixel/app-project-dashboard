package com.skillgap.controller;

import com.skillgap.entity.User;
import com.skillgap.entity.enums.Role;
import com.skillgap.exception.BusinessException;
import com.skillgap.repository.AssessmentAttemptRepository;
import com.skillgap.repository.AuditLogRepository;
import com.skillgap.repository.InputDocumentRepository;
import com.skillgap.repository.UserRepository;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.AdminService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin panel. Every route is protected by /admin/** hasRole("ADMIN) in
 * SecurityConfig - server-side, not just hidden buttons.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CurrentUserService currentUserService;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final InputDocumentRepository inputDocumentRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(CurrentUserService currentUserService,
                           AdminService adminService,
                           UserRepository userRepository,
                           AssessmentAttemptRepository attemptRepository,
                           InputDocumentRepository inputDocumentRepository,
                           AuditLogRepository auditLogRepository) {
        this.currentUserService = currentUserService;
        this.adminService = adminService;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.inputDocumentRepository = inputDocumentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("skillCount", adminService.listSkills().size());
        model.addAttribute("roleCount", adminService.listRoles().size());
        model.addAttribute("assessmentCount", adminService.listAssessments().size());
        model.addAttribute("resourceCount", adminService.listResources().size());
        model.addAttribute("documentCount", inputDocumentRepository.count());
        model.addAttribute("attemptCount", attemptRepository.count());
        model.addAttribute("recentAudit", auditLogRepository.findTop100ByOrderByCreatedAtDesc());
        return "admin/index";
    }

    // -------------------------------------------------------------------- users

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", adminService.listUsers());
        return "admin/users";
    }

    @PostMapping("/users/{userId}")
    public String updateUser(@PathVariable Long userId,
                             @RequestParam("role") Role role,
                             @RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.updateUser(currentUserService.getUser(), userId, role, enabled);
            redirectAttributes.addFlashAttribute("message", "User updated.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // -------------------------------------------------------------- categories

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", adminService.listCategories());
        return "admin/categories";
    }

    @PostMapping("/categories/add")
    public String addCategory(@RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) String description,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.createCategory(currentUserService.getUser(), name, description);
            redirectAttributes.addFlashAttribute("message", "Category added.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteCategory(currentUserService.getUser(), id);
            redirectAttributes.addFlashAttribute("message", "Category deleted.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ------------------------------------------------------------------- skills

    @GetMapping("/skills")
    public String skills(Model model) {
        model.addAttribute("skills", adminService.listSkills());
        model.addAttribute("categories", adminService.listCategories());
        return "admin/skills";
    }

    @PostMapping("/skills/add")
    public String addSkill(@RequestParam("name") String name,
                           @RequestParam(value = "categoryId", required = false) Long categoryId,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "aliases", required = false) String aliases,
                           RedirectAttributes redirectAttributes) {
        try {
            adminService.createSkill(currentUserService.getUser(), name, categoryId, description, aliases);
            redirectAttributes.addFlashAttribute("message", "Skill added.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/skills";
    }

    @PostMapping("/skills/{id}/update")
    public String updateSkill(@PathVariable Long id,
                              @RequestParam(value = "name", required = false) String name,
                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "aliases", required = false) String aliases,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.updateSkill(currentUserService.getUser(), id, name, categoryId, description, aliases);
            redirectAttributes.addFlashAttribute("message", "Skill updated.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/skills";
    }

    @PostMapping("/skills/{id}/delete")
    public String deleteSkill(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteSkill(currentUserService.getUser(), id);
            redirectAttributes.addFlashAttribute("message", "Skill deleted.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/skills";
    }

    // ---------------------------------------------------------------- job roles

    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("roles", adminService.listRoles());
        model.addAttribute("skills", adminService.listSkills());
        return "admin/roles";
    }

    @PostMapping("/roles/add")
    public String addRole(@RequestParam("name") String name,
                          @RequestParam(value = "description", required = false) String description,
                          RedirectAttributes redirectAttributes) {
        try {
            adminService.createRole(currentUserService.getUser(), name, description);
            redirectAttributes.addFlashAttribute("message", "Job role created.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/roles/{id}/update")
    public String updateRole(@PathVariable Long id,
                             @RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "description", required = false) String description,
                             RedirectAttributes redirectAttributes) {
        try {
            adminService.updateRole(currentUserService.getUser(), id, name, description);
            redirectAttributes.addFlashAttribute("message", "Job role updated.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/roles/{id}/skills/set")
    public String setRoleSkill(@PathVariable Long id,
                               @RequestParam("skillId") Long skillId,
                               @RequestParam("requiredLevel") int requiredLevel,
                               @RequestParam("weight") int weight,
                               @RequestParam("type") com.skillgap.entity.enums.RequirementType type,
                               RedirectAttributes redirectAttributes) {
        try {
            adminService.setRoleSkill(currentUserService.getUser(), id, skillId, requiredLevel, weight, type);
            redirectAttributes.addFlashAttribute("message", "Role requirement saved.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles/" + id;
    }

    @PostMapping("/roles/{id}/skills/{skillId}/remove")
    public String removeRoleSkill(@PathVariable Long id, @PathVariable Long skillId,
                                  RedirectAttributes redirectAttributes) {
        try {
            adminService.removeRoleSkill(currentUserService.getUser(), id, skillId);
            redirectAttributes.addFlashAttribute("message", "Requirement removed.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles/" + id;
    }

    @PostMapping("/roles/{id}/delete")
    public String deleteRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteRole(currentUserService.getUser(), id);
            redirectAttributes.addFlashAttribute("message", "Job role deleted.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @GetMapping("/roles/{id}")
    public String roleDetail(@PathVariable Long id, Model model) {
        model.addAttribute("role", adminService.getRole(id));
        model.addAttribute("skills", adminService.listSkills());
        return "admin/role-detail";
    }

    // -------------------------------------------------------------- assessments

    @GetMapping("/assessments")
    public String assessments(Model model) {
        model.addAttribute("assessments", adminService.listAssessments());
        model.addAttribute("skills", adminService.listSkills());
        return "admin/assessments";
    }

    @PostMapping("/assessments/add")
    public String addAssessment(@RequestParam("title") String title,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam("skillId") Long skillId,
                                @RequestParam("passingScore") int passingScore,
                                @RequestParam(value = "active", defaultValue = "false") boolean active,
                                RedirectAttributes redirectAttributes) {
        try {
            adminService.createAssessment(currentUserService.getUser(), title, description, skillId,
                    passingScore, active);
            redirectAttributes.addFlashAttribute("message", "Assessment created. Add questions below.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/assessments";
    }

    @PostMapping("/assessments/{id}/status")
    public String assessmentStatus(@PathVariable Long id,
                                   @RequestParam("active") boolean active,
                                   RedirectAttributes redirectAttributes) {
        adminService.setAssessmentActive(currentUserService.getUser(), id, active);
        redirectAttributes.addFlashAttribute("message", "Assessment status updated.");
        return "redirect:/admin/assessments";
    }

    @PostMapping("/assessments/{id}/questions/add")
    public String addQuestion(@PathVariable Long id,
                              @RequestParam("questionText") String questionText,
                              @RequestParam("option1") String option1,
                              @RequestParam("option2") String option2,
                              @RequestParam(value = "option3", required = false) String option3,
                              @RequestParam(value = "option4", required = false) String option4,
                              @RequestParam("correctIndex") int correctIndex,
                              RedirectAttributes redirectAttributes) {
        try {
            List<String> options = new java.util.ArrayList<>(List.of(option1, option2));
            if (option3 != null && !option3.isBlank()) {
                options.add(option3);
            }
            if (option4 != null && !option4.isBlank()) {
                options.add(option4);
            }
            adminService.addQuestion(currentUserService.getUser(), id, questionText, options, correctIndex);
            redirectAttributes.addFlashAttribute("message", "Question added.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/assessments";
    }

    // -------------------------------------------------------- learning resources

    @GetMapping("/resources")
    public String resources(Model model) {
        model.addAttribute("resources", adminService.listResources());
        model.addAttribute("skills", adminService.listSkills());
        return "admin/resources";
    }

    @PostMapping("/resources/add")
    public String addResource(@RequestParam("title") String title,
                              @RequestParam("url") String url,
                              @RequestParam(value = "skillId", required = false) Long skillId,
                              @RequestParam("difficulty") int difficulty,
                              @RequestParam(value = "estimatedHours", required = false) Double estimatedHours,
                              @RequestParam(value = "description", required = false) String description,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.createResource(currentUserService.getUser(), title, url, skillId, difficulty,
                    estimatedHours, description);
            redirectAttributes.addFlashAttribute("message", "Resource added.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/resources";
    }

    @PostMapping("/resources/{id}/status")
    public String resourceStatus(@PathVariable Long id,
                                 @RequestParam("active") boolean active,
                                 RedirectAttributes redirectAttributes) {
        adminService.setResourceActive(currentUserService.getUser(), id, active);
        redirectAttributes.addFlashAttribute("message", "Resource status updated.");
        return "redirect:/admin/resources";
    }
}
