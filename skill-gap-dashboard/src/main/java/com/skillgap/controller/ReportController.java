package com.skillgap.controller;

import com.skillgap.entity.User;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** Printable skill-gap report for one analysis. Access is owner-checked. */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final CurrentUserService currentUserService;
    private final ReportService reportService;
    private final GapAnalysisService gapAnalysisService;

    public ReportController(CurrentUserService currentUserService,
                            ReportService reportService,
                            GapAnalysisService gapAnalysisService) {
        this.currentUserService = currentUserService;
        this.reportService = reportService;
        this.gapAnalysisService = gapAnalysisService;
    }

    /** Report list: every saved analysis of the current user. */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("sessions", gapAnalysisService.history(currentUserService.getUser()));
        return "reports/list";
    }

    @GetMapping("/{sessionId}")
    public String view(@PathVariable Long sessionId, Model model) {
        User user = currentUserService.getUser();
        ReportService.Report report = reportService.buildReport(user, sessionId);
        model.addAttribute("report", report);
        model.addAttribute("profile", user);
        return "reports/view";
    }
}
