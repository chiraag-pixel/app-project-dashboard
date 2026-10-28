package com.skillgap.controller;

import com.skillgap.dto.WorkSpecForm;
import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.RequirementType;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.service.SkillCatalogService;
import com.skillgap.service.WorkSpecService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Work specifications and custom job descriptions with requirement review. */
@Controller
@RequestMapping("/work-specs")
public class WorkSpecController {

    private final CurrentUserService currentUserService;
    private final WorkSpecService workSpecService;
    private final SkillCatalogService skillCatalogService;
    private final GapAnalysisService gapAnalysisService;

    public WorkSpecController(CurrentUserService currentUserService,
                              WorkSpecService workSpecService,
                              SkillCatalogService skillCatalogService,
                              GapAnalysisService gapAnalysisService) {
        this.currentUserService = currentUserService;
        this.workSpecService = workSpecService;
        this.skillCatalogService = skillCatalogService;
        this.gapAnalysisService = gapAnalysisService;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUserService.getUser();
        if (!model.containsAttribute("workSpecForm")) {
            model.addAttribute("workSpecForm", new WorkSpecForm());
        }
        model.addAttribute("workSpecs", workSpecService.listWorkSpecs(user));
        model.addAttribute("customJobDescriptions", user == null ? java.util.List.of()
                : workSpecService.listForUser(user).stream()
                        .filter(d -> d.getDocumentType() == com.skillgap.entity.enums.DocumentType.JOB_DESCRIPTION)
                        .toList());
        return "workspecs/list";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("workSpecForm") WorkSpecForm form,
                      BindingResult bindingResult,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("workSpecs", workSpecService.listWorkSpecs(user));
            return "workspecs/list";
        }
        var document = workSpecService.createWorkSpec(user, form);
        redirectAttributes.addFlashAttribute("message",
                "Work specification saved. Review the suggested skill requirements.");
        return "redirect:/work-specs/" + document.getId();
    }

    @PostMapping("/custom-job/add")
    public String addCustomJob(@RequestParam("title") String title,
                               @RequestParam("description") String description,
                               RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            var document = workSpecService.createCustomJobDescription(user, title, description);
            redirectAttributes.addFlashAttribute("message",
                    "Job description saved. Review the suggested skill requirements.");
            return "redirect:/work-specs/" + document.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/work-specs";
        }
    }

    /** Requirement review page for one work spec / custom job description. */
    @GetMapping("/{id}")
    public String review(@PathVariable Long id, Model model) {
        User user = currentUserService.getUser();
        var document = workSpecService.getOwned(user, id);
        model.addAttribute("document", document);
        model.addAttribute("requirements", workSpecService.listRequirements(user, id));
        model.addAttribute("catalogSkills", skillCatalogService.listSkills());
        return "workspecs/review";
    }

    @PostMapping("/{id}/requirements/{requirementId}/confirm")
    public String confirmRequirement(@PathVariable Long id, @PathVariable Long requirementId,
                                     @RequestParam(value = "level", required = false) Integer level,
                                     @RequestParam(value = "weight", required = false) Integer weight,
                                     @RequestParam(value = "type", required = false) RequirementType type,
                                     RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            workSpecService.updateRequirement(user, requirementId, level, weight, type, VerificationStatus.CONFIRMED);
            redirectAttributes.addFlashAttribute("message", "Requirement confirmed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/work-specs/" + id;
    }

    @PostMapping("/{id}/requirements/{requirementId}/update")
    public String updateRequirement(@PathVariable Long id, @PathVariable Long requirementId,
                                    @RequestParam(value = "level", required = false) Integer level,
                                    @RequestParam(value = "weight", required = false) Integer weight,
                                    @RequestParam(value = "type", required = false) RequirementType type,
                                    RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            workSpecService.updateRequirement(user, requirementId, level, weight, type, null);
            redirectAttributes.addFlashAttribute("message", "Requirement updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/work-specs/" + id;
    }

    @PostMapping("/{id}/requirements/{requirementId}/reject")
    public String rejectRequirement(@PathVariable Long id, @PathVariable Long requirementId,
                                    RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            workSpecService.updateRequirement(user, requirementId, null, null, null, VerificationStatus.REJECTED);
            redirectAttributes.addFlashAttribute("message", "Requirement rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/work-specs/" + id;
    }

    @PostMapping("/{id}/requirements/add")
    public String addRequirement(@PathVariable Long id,
                                 @RequestParam(value = "skillId", required = false) Long skillId,
                                 @RequestParam(value = "newSkillName", required = false) String newSkillName,
                                 @RequestParam("level") Integer level,
                                 @RequestParam(value = "weight", required = false) Integer weight,
                                 @RequestParam(value = "type", required = false) RequirementType type,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            workSpecService.addRequirement(user, id, skillId, newSkillName, level, weight, type);
            redirectAttributes.addFlashAttribute("message", "Requirement added.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/work-specs/" + id;
    }

    @PostMapping("/{id}/analyze")
    public String analyze(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            var document = workSpecService.getOwned(user, id);
            AnalysisSession session = gapAnalysisService.analyzeDocument(user, document);
            return "redirect:/analysis/" + session.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/work-specs/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        workSpecService.delete(currentUserService.getUser(), id);
        redirectAttributes.addFlashAttribute("message", "Specification deleted.");
        return "redirect:/work-specs";
    }
}
