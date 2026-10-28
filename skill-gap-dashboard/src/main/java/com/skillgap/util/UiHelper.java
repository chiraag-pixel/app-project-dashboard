package com.skillgap.util;

import com.skillgap.entity.enums.Priority;
import com.skillgap.entity.enums.TaskStatus;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Small formatting helpers exposed to Thymeleaf as {@code @ui}. */
@Component("ui")
public class UiHelper {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

    /** 1 -> "1 - Beginner", 5 -> "5 - Expert", null -> "Not rated". */
    public String levelLabel(Integer level) {
        if (level == null) {
            return "Not rated";
        }
        return switch (level) {
            case 1 -> "1 - Beginner";
            case 2 -> "2 - Basic";
            case 3 -> "3 - Intermediate";
            case 4 -> "4 - Advanced";
            case 5 -> "5 - Expert";
            default -> level + " - Unknown";
        };
    }

    public String priorityBadge(Priority priority) {
        if (priority == null) {
            return "badge-low";
        }
        return switch (priority) {
            case MET -> "badge-met";
            case CRITICAL -> "badge-critical";
            case HIGH -> "badge-high";
            case MEDIUM -> "badge-medium";
            case LOW -> "badge-low";
        };
    }

    public String taskStatusLabel(TaskStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case NOT_STARTED -> "Not started";
            case IN_PROGRESS -> "In progress";
            case COMPLETED -> "Completed";
        };
    }

    /** Score percent -> proficiency level label (mirrors AssessmentService rules). */
    public String levelLabelForScore(Integer scorePercent) {
        if (scorePercent == null) {
            return "Not rated";
        }
        int level = scorePercent >= 90 ? 5 : scorePercent >= 75 ? 4 : scorePercent >= 55 ? 3 : scorePercent >= 35 ? 2 : 1;
        return levelLabel(level);
    }

    /** Inclusive integer range for dropdowns, e.g. range(1, 5) -> 1,2,3,4,5. */
    public java.util.List<Integer> range(int from, int to) {
        java.util.List<Integer> values = new java.util.ArrayList<>();
        for (int i = from; i <= to; i++) {
            values.add(i);
        }
        return values;
    }

    /** Shortens long text for safe display; never throws on short/null input. */
    public String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String flat = text.trim();
        if (flat.length() <= maxLength) {
            return flat;
        }
        return flat.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    /** Correct answer text of a question ("-" when the quiz data is incomplete). */
    public String correctAnswer(com.skillgap.entity.AssessmentQuestion question) {
        if (question == null || question.getOptions() == null) {
            return "-";
        }
        return question.getOptions().stream()
                .filter(com.skillgap.entity.AssessmentOption::isCorrect)
                .map(com.skillgap.entity.AssessmentOption::getOptionText)
                .findFirst()
                .orElse("-");
    }

    public String dateTime(Instant instant) {
        return instant == null ? "-" : DATE_TIME.format(instant);
    }

    public String date(Instant instant) {
        return instant == null ? "-" : DATE.format(instant);
    }

    /** Rounds to one decimal for percentages. */
    public String pct(Double value) {
        return value == null ? "-" : String.format("%.1f%%", value);
    }
}
