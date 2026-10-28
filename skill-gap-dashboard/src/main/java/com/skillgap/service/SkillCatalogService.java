package com.skillgap.service;

import com.skillgap.entity.Skill;
import com.skillgap.entity.SkillCategory;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.SkillCategoryRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.util.SkillNormalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Access to the shared skill catalog (categories + skills) and text matching. */
@Service
public class SkillCatalogService {

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;

    public SkillCatalogService(SkillRepository skillRepository, SkillCategoryRepository categoryRepository) {
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
    }

    // ---------------------------------------------------------------- categories

    @Transactional(readOnly = true)
    public List<SkillCategory> listCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public SkillCategory createCategory(String name, String description) {
        String trimmed = name.trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmed)) {
            throw new BusinessException("A category named '" + trimmed + "' already exists.");
        }
        return categoryRepository.save(new SkillCategory(trimmed, trimToNull(description)));
    }

    @Transactional
    public void deleteCategory(Long id) {
        SkillCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        long used = skillRepository.findByCategoryIdOrderByNameAsc(id).size();
        if (used > 0) {
            throw new BusinessException("Cannot delete: " + used + " skill(s) still use this category.");
        }
        categoryRepository.delete(category);
    }

    // ---------------------------------------------------------------------- skills

    @Transactional(readOnly = true)
    public List<Skill> listSkills() {
        return skillRepository.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public Skill getSkill(Long id) {
        return skillRepository.findById(id).orElseThrow(() -> new NotFoundException("Skill not found"));
    }

    @Transactional(readOnly = true)
    public List<Skill> searchSkills(String query) {
        if (query == null || query.isBlank()) {
            return listSkills();
        }
        return skillRepository.findByNameContainingIgnoreCase(query.trim());
    }

    /**
     * Finds a skill by exact normalized name/alias, or creates a new catalog entry.
     *
     * @param autoCreated receives true when a new catalog skill was created
     */
    @Transactional
    public Skill findOrCreate(String name, SkillCategory category, boolean[] autoCreated) {
        String normalized = SkillNormalizer.normalize(name);
        if (normalized.isEmpty()) {
            throw new BusinessException("Skill name is required.");
        }
        Optional<Skill> existing = skillRepository.findByNormalizedName(normalized);
        if (existing.isPresent()) {
            if (autoCreated != null && autoCreated.length > 0) {
                autoCreated[0] = false;
            }
            return existing.get();
        }
        Skill skill = new Skill(name.trim(), normalized, category);
        skillRepository.save(skill);
        if (autoCreated != null && autoCreated.length > 0) {
            autoCreated[0] = true;
        }
        return skill;
    }

    // -------------------------------------------------------------- text matching

    /**
     * Scans a text for catalog skill terms.
     *
     * @return matches ordered by first occurrence; each entry is the Skill
     */
    @Transactional(readOnly = true)
    public List<TermMatch> matchTerms(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        // Search is case-insensitive; separators like "spring boot"/"spring-boot"/"SpringBoot" all match.
        String haystack = text.toLowerCase();
        List<TermMatch> matches = new ArrayList<>();
        for (Skill skill : skillRepository.findAllOrdered()) {
            TermMatch best = null;
            for (String term : allTerms(skill)) {
                if (term.length() < 2) {
                    continue;
                }
                Pattern pattern = patternFor(term);
                java.util.regex.Matcher m = pattern.matcher(haystack);
                if (m.find()) {
                    double confidence = term.equalsIgnoreCase(skill.getName()) ? 0.9 : 0.7;
                    String snippet = snippet(text, m.start(), m.end());
                    if (best == null || confidence > best.confidence()) {
                        best = new TermMatch(skill, confidence, snippet, m.start());
                    }
                }
            }
            if (best != null) {
                matches.add(best);
            }
        }
        matches.sort(java.util.Comparator.comparingInt(TermMatch::position));
        return matches;
    }

    private List<String> allTerms(Skill skill) {
        List<String> terms = new ArrayList<>();
        terms.add(skill.getName());
        if (skill.getAliases() != null && !skill.getAliases().isBlank()) {
            for (String alias : skill.getAliases().split(",")) {
                if (!alias.isBlank()) {
                    terms.add(alias.trim());
                }
            }
        }
        return terms;
    }

    /**
     * Builds a word-boundary regex for a term, so "java" does not match inside
     * "javascript" and "spring boot" also matches "spring-boot" or "SpringBoot".
     */
    private Pattern patternFor(String term) {
        String[] parts = term.toLowerCase().split("[\\s._\\-/]+");
        StringBuilder regex = new StringBuilder("(?<![a-z0-9+#])");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append("[\\s._\\-/]*");
            }
            regex.append(java.util.regex.Pattern.quote(parts[i]));
        }
        regex.append("(?![a-z0-9+#])");
        return Pattern.compile(regex.toString());
    }

    private String snippet(String text, int start, int end) {
        int from = Math.max(0, start - 70);
        int to = Math.min(text.length(), end + 70);
        String fragment = text.substring(from, to).replaceAll("\\s+", " ").trim();
        if (from > 0) {
            fragment = "…" + fragment;
        }
        if (to < text.length()) {
            fragment = fragment + "…";
        }
        return fragment;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** One detected term occurrence. */
    public record TermMatch(Skill skill, double confidence, String snippet, int position) {
    }
}
