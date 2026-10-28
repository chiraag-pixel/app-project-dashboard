package com.skillgap.controller;

import com.skillgap.dto.UserSkillForm;
import com.skillgap.entity.User;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.SkillCatalogService;
import com.skillgap.service.UserSkillService;
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

/** My Skills: manual entry, edit and delete. */
@Controller
@RequestMapping("/skills")
public class SkillController {

    private final CurrentUserService currentUserService;
    private final UserSkillService userSkillService;
    private final SkillCatalogService skillCatalogService;

    public SkillController(CurrentUserService currentUserService,
                           UserSkillService userSkillService,
                           SkillCatalogService skillCatalogService) {
        this.currentUserService = currentUserService;
        this.userSkillService = userSkillService;
        this.skillCatalogService = skillCatalogService;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUserService.getUser();
        if (!model.containsAttribute("skillForm")) {
            model.addAttribute("skillForm", new UserSkillForm());
        }
        model.addAttribute("userSkills", userSkillService.listForUser(user));
        model.addAttribute("catalogSkills", skillCatalogService.listSkills());
        model.addAttribute("categories", skillCatalogService.listCategories());
        return "skills/list";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("skillForm") UserSkillForm form,
                      BindingResult bindingResult,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("userSkills", userSkillService.listForUser(user));
            model.addAttribute("catalogSkills", skillCatalogService.listSkills());
            model.addAttribute("categories", skillCatalogService.listCategories());
            return "skills/list";
        }
        userSkillService.addSkill(user, form);
        redirectAttributes.addFlashAttribute("message", "Skill saved to your profile.");
        return "redirect:/skills";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("skillForm") UserSkillForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("userSkills", userSkillService.listForUser(user));
            model.addAttribute("catalogSkills", skillCatalogService.listSkills());
            model.addAttribute("categories", skillCatalogService.listCategories());
            model.addAttribute("editingId", id);
            return "skills/list";
        }
        userSkillService.updateSkill(user, id, form);
        redirectAttributes.addFlashAttribute("message", "Skill updated.");
        return "redirect:/skills";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userSkillService.deleteSkill(currentUserService.getUser(), id);
        redirectAttributes.addFlashAttribute("message", "Skill removed.");
        return "redirect:/skills";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        var userSkill = userSkillService.getOwned(user, id);
        UserSkillForm form = new UserSkillForm();
        form.setSkillId(userSkill.getSkill().getId());
        form.setLevel(userSkill.getLevel());
        form.setYearsOfExperience(userSkill.getYearsOfExperience());
        form.setNotes(userSkill.getNotes());
        model.addAttribute("skillForm", form);
        model.addAttribute("editingId", id);
        model.addAttribute("editingSkillName", userSkill.getSkill().getName());
        model.addAttribute("userSkills", userSkillService.listForUser(user));
        model.addAttribute("catalogSkills", skillCatalogService.listSkills());
        model.addAttribute("categories", skillCatalogService.listCategories());
        return "skills/list";
    }
}
