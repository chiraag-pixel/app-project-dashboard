package com.skillgap.dto;

import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.AssessmentAttempt;
import com.skillgap.entity.GapAnalysis;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.JobRole;
import java.util.List;

/** Everything the dashboard page needs - all values come from the database. */
public record DashboardData(
        long skillCount,
        long assessmentCount,
        long roadmapTaskCount,
        JobRole targetRole,
        AnalysisSession latestSession,
        GapAnalysis latestAnalysis,
        List<GapAnalysisDetail> topGaps,
        List<AssessmentAttempt> recentAttempts,
        List<AnalysisSession> recentSessions,
        double roadmapProgressPercent,
        boolean hasAnyData) {
}
