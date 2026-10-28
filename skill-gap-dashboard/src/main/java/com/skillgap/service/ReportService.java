package com.skillgap.service;

import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.GapAnalysis;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the printable report model for one saved analysis (owner-checked). */
@Service
public class ReportService {

    private final GapAnalysisService gapAnalysisService;

    public ReportService(GapAnalysisService gapAnalysisService) {
        this.gapAnalysisService = gapAnalysisService;
    }

    @Transactional(readOnly = true)
    public Report buildReport(User user, Long sessionId) {
        AnalysisSession session = gapAnalysisService.getSessionOwned(user, sessionId);
        GapAnalysis analysis = gapAnalysisService.getAnalysisForSession(session.getId());
        List<GapAnalysisDetail> details = gapAnalysisService.getDetails(analysis.getId());
        return new Report(user, session, analysis, details);
    }

    /** Report view model. */
    public record Report(User user, AnalysisSession session, GapAnalysis analysis, List<GapAnalysisDetail> details) {

        public List<GapAnalysisDetail> strengths() {
            return details.stream().filter(d -> d.getPriority() == com.skillgap.entity.enums.Priority.MET).toList();
        }

        public List<GapAnalysisDetail> gaps() {
            return details.stream().filter(d -> d.getPriority() != com.skillgap.entity.enums.Priority.MET).toList();
        }

        public List<GapAnalysisDetail> unassessed() {
            return details.stream().filter(d -> !d.isAssessed()).toList();
        }
    }
}
