package com.skillgap.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes skill names so that "Spring  Boot", "spring-boot" and
 * "SPRINGBOOT" all map to the same catalog entry.
 */
public final class SkillNormalizer {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private SkillNormalizer() {
    }

    /** Canonical stored form: trimmed, lower-case, single spaces. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return MULTI_SPACE.matcher(raw.trim().toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    /** Matching key: normalized with all separators removed, e.g. "spring-boot" -> "springboot". */
    public static String compactKey(String raw) {
        return normalize(raw).replaceAll("[\\s._\\-/+#]+", "");
    }
}
