package com.skillgap.controller;

import com.skillgap.entity.AssessmentAttempt;
import com.skillgap.entity.User;
import com.skillgap.exception.BusinessException;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.AssessmentService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Taking assessments and reviewing results. */
@Controller
@RequestMapping("/assessments")
public class AssessmentController {

    private final CurrentUserService currentUserService;
    private final AssessmentService assessmentService;

    public AssessmentController(CurrentUserService currentUserService,
                                AssessmentService assessmentService) {
        this.currentUserService = currentUserService;
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUserService.getUser();
        model.addAttribute("assessments", assessmentService.listActive());
        model.addAttribute("attempts", assessmentService.attemptsForUser(user));
        return "assessments/list";
    }

    @GetMapping("/{id}/take")
    public String take(@PathVariable Long id, Model model) {
        var assessment = assessmentService.getActive(id);
        var questions = assessmentService.questionsOf(id);
        if (questions.isEmpty()) {
            model.addAttribute("errorMessage", "This assessment has no questions yet.");
            model.addAttribute("assessments", assessmentService.listActive());
            model.addAttribute("attempts", assessmentService.attemptsForUser(currentUserService.getUser()));
            return "assessments/list";
        }
        model.addAttribute("assessment", assessment);
        model.addAttribute("questions", questions);
        return "assessments/take";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id,
                         @RequestParam Map<String, String> params,
                         RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            Map<Long, Long> answers = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("question_") && entry.getValue() != null && !entry.getValue().isBlank()) {
                    answers.put(Long.parseLong(entry.getKey().substring("question_".length())),
                            Long.parseLong(entry.getValue()));
                }
            }
            AssessmentAttempt attempt = assessmentService.submit(user, id, answers);
            return "redirect:/assessments/attempts/" + attempt.getId();
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/assessments/" + id + "/take";
        }
    }

    @GetMapping("/attempts/{attemptId}")
    public String result(@PathVariable Long attemptId, Model model) {
        User user = currentUserService.getUser();
        AssessmentAttempt attempt = assessmentService.getAttemptOwned(user, attemptId);
        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", assessmentService.answersOf(attemptId));
        return "assessments/result";
    }

    @PostMapping("/attempts/{attemptId}/apply")
    public String apply(@PathVariable Long attemptId, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            var userSkill = assessmentService.applyToProfile(user, attemptId);
            redirectAttributes.addFlashAttribute("message", "Score applied to your profile: "
                    + userSkill.getSkill().getName() + " is now level " + userSkill.getLevel() + ".");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assessments/attempts/" + attemptId;
    }
}
