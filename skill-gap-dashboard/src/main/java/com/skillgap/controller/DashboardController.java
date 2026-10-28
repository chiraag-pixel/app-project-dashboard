package com.skillgap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgap.dto.DashboardData;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.User;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.DashboardService;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.service.NotificationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Dashboard showing real metrics from the database. */
@Controller
public class DashboardController {

    private final CurrentUserService currentUserService;
    private final DashboardService dashboardService;
    private final GapAnalysisService gapAnalysisService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public DashboardController(CurrentUserService currentUserService,
                               DashboardService dashboardService,
                               GapAnalysisService gapAnalysisService,
                               NotificationService notificationService,
                               ObjectMapper objectMapper) {
        this.currentUserService = currentUserService;
        this.dashboardService = dashboardService;
        this.gapAnalysisService = gapAnalysisService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /** "/" simply forwards visitors to the dashboard (or to login, if anonymous). */
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) throws Exception {
        User user = currentUserService.getUser();
        DashboardData data = dashboardService.buildFor(user);

        // Chart data for "current vs required" comes from the latest saved analysis.
        List<String> labels = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        List<Integer> required = new ArrayList<>();
        if (data.latestAnalysis() != null) {
            for (GapAnalysisDetail detail : gapAnalysisService.getDetails(data.latestAnalysis().getId())) {
                labels.add(detail.getSkill().getName());
                current.add(detail.getCurrentLevel() == null ? 0 : detail.getCurrentLevel());
                required.add(detail.getRequiredLevel());
            }
        }
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("labels", labels);
        chart.put("current", current);
        chart.put("required", required);

        model.addAttribute("data", data);
        model.addAttribute("chartData", objectMapper.writeValueAsString(chart));
        model.addAttribute("unreadNotifications", notificationService.unreadCount(user));
        return "dashboard";
    }
}
