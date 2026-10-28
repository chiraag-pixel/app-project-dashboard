package com.skillgap.controller;

import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.GapAnalysis;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.User;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.service.JobRoleQueryService;
import com.skillgap.service.WorkSpecService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Run, view and browse skill gap analyses. */
@Controller
@RequestMapping("/analysis")
public class AnalysisController {

    private final CurrentUserService currentUserService;
    private final GapAnalysisService gapAnalysisService;
    private final JobRoleQueryService jobRoleQueryService;
    private final WorkSpecService workSpecService;

    public AnalysisController(CurrentUserService currentUserService,
                              GapAnalysisService gapAnalysisService,
                              JobRoleQueryService jobRoleQueryService,
                              WorkSpecService workSpecService) {
        this.currentUserService = currentUserService;
        this.gapAnalysisService = gapAnalysisService;
        this.jobRoleQueryService = jobRoleQueryService;
        this.workSpecService = workSpecService;
    }

    /** Start a new analysis: choose a job role or a saved work specification. */
    @GetMapping
    public String start(Model model) {
        User user = currentUserService.getUser();
        model.addAttribute("roles", jobRoleQueryService.listRoles());
        model.addAttribute("workSpecs", workSpecService.listWorkSpecs(user));
        model.addAttribute("customJobs", workSpecService.listForUser(user).stream()
                .filter(d -> d.getDocumentType() == com.skillgap.entity.enums.DocumentType.JOB_DESCRIPTION)
                .toList());
        model.addAttribute("history", gapAnalysisService.history(user));
        return "analysis/new";
    }

    @PostMapping("/run/role")
    public String runForRole(@RequestParam("jobRoleId") Long jobRoleId,
                             RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            AnalysisSession session = gapAnalysisService.analyzeJobRole(user, jobRoleId);
            return "redirect:/analysis/" + session.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/analysis";
        }
    }

    @PostMapping("/run/document")
    public String runForDocument(@RequestParam("documentId") Long documentId,
                                 RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            var document = workSpecService.getOwned(user, documentId);
            AnalysisSession session = gapAnalysisService.analyzeDocument(user, document);
            return "redirect:/analysis/" + session.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/analysis";
        }
    }

    @GetMapping("/{sessionId}")
    public String result(@PathVariable Long sessionId, Model model) {
        User user = currentUserService.getUser();
        AnalysisSession session = gapAnalysisService.getSessionOwned(user, sessionId);
        GapAnalysis analysis = gapAnalysisService.getAnalysisForSession(sessionId);
        List<GapAnalysisDetail> details = gapAnalysisService.getDetails(analysis.getId());

        List<GapAnalysisDetail> strengths = new ArrayList<>();
        List<GapAnalysisDetail> partial = new ArrayList<>();
        List<GapAnalysisDetail> unassessed = new ArrayList<>();
        List<GapAnalysisDetail> critical = new ArrayList<>();
        for (GapAnalysisDetail detail : details) {
            if (detail.getPriority() == com.skillgap.entity.enums.Priority.MET) {
                strengths.add(detail);
            } else {
                partial.add(detail);
                if (!detail.isAssessed()) {
                    unassessed.add(detail);
                }
                if (detail.getPriority() == com.skillgap.entity.enums.Priority.CRITICAL) {
                    critical.add(detail);
                }
            }
        }

        // Worst-first improvement order: CRITICAL -> HIGH -> MEDIUM -> LOW, then gap %.
        partial.sort(java.util.Comparator
                .comparingInt((GapAnalysisDetail d) -> d.getPriority().ordinal()).reversed()
                .thenComparing(GapAnalysisDetail::getGapPercentage, Comparator.reverseOrder()));

        model.addAttribute("session", session);
        model.addAttribute("analysis", analysis);
        model.addAttribute("details", details);
        model.addAttribute("strengths", strengths);
        model.addAttribute("gaps", partial);
        model.addAttribute("unassessed", unassessed);
        model.addAttribute("critical", critical);
        return "analysis/result";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("history", gapAnalysisService.history(currentUserService.getUser()));
        return "analysis/history";
    }
}
