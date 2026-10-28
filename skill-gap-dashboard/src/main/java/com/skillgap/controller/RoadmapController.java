package com.skillgap.controller;

import com.skillgap.dto.RoadmapTaskForm;
import com.skillgap.entity.LearningRoadmap;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.TaskStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.service.RoadmapService;
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

/** Learning roadmap: generate from an analysis and track task completion. */
@Controller
@RequestMapping("/roadmap")
public class RoadmapController {

    private final CurrentUserService currentUserService;
    private final RoadmapService roadmapService;
    private final GapAnalysisService gapAnalysisService;

    public RoadmapController(CurrentUserService currentUserService,
                             RoadmapService roadmapService,
                             GapAnalysisService gapAnalysisService) {
        this.currentUserService = currentUserService;
        this.roadmapService = roadmapService;
        this.gapAnalysisService = gapAnalysisService;
    }

    @GetMapping
    public String view(Model model) {
        User user = currentUserService.getUser();
        LearningRoadmap roadmap = roadmapService.currentRoadmap(user);
        if (!model.containsAttribute("taskForm")) {
            model.addAttribute("taskForm", new RoadmapTaskForm());
        }
        model.addAttribute("roadmap", roadmap);
        if (roadmap != null) {
            model.addAttribute("tasks", roadmapService.tasksOf(roadmap.getId()));
            model.addAttribute("progress", roadmapService.progressPercent(roadmap.getId()));
        }
        model.addAttribute("sessions", gapAnalysisService.history(user));
        return "roadmap/view";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam("sessionId") Long sessionId, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            var session = gapAnalysisService.getSessionOwned(user, sessionId);
            roadmapService.generateFromAnalysis(user, session);
            redirectAttributes.addFlashAttribute("message",
                    "Roadmap generated from your latest gaps (replaces the previous plan).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/roadmap";
    }

    @PostMapping("/tasks/add")
    public String addTask(@Valid @ModelAttribute("taskForm") RoadmapTaskForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please check the task details.");
            return "redirect:/roadmap";
        }
        roadmapService.addTask(user, form);
        redirectAttributes.addFlashAttribute("message", "Task added.");
        return "redirect:/roadmap";
    }

    @PostMapping("/tasks/{taskId}/status")
    public String status(@PathVariable Long taskId,
                         @RequestParam("status") TaskStatus status,
                         RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            roadmapService.changeStatus(user, taskId, status);
            redirectAttributes.addFlashAttribute("message", "Task status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/roadmap";
    }

    @PostMapping("/tasks/{taskId}/delete")
    public String delete(@PathVariable Long taskId, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            roadmapService.deleteTask(user, taskId);
            redirectAttributes.addFlashAttribute("message", "Task removed.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/roadmap";
    }
}
