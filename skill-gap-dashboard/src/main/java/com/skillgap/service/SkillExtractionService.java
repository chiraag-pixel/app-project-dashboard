package com.skillgap.service;

import com.skillgap.entity.Skill;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Rule-based skill extraction: matches text against the skill catalog and its
 * aliases. This service is intentionally small and self-contained so a future
 * AI-based extractor could be added behind the same interface.
 */
@Service
public class SkillExtractionService {

    private final SkillCatalogService skillCatalogService;

    public SkillExtractionService(SkillCatalogService skillCatalogService) {
        this.skillCatalogService = skillCatalogService;
    }

    /** Detects catalog skills in a text. Extracted text is treated as untrusted data. */
    public List<Candidate> extractCandidates(String text) {
        return skillCatalogService.matchTerms(text).stream()
                .map(match -> new Candidate(match.skill(), match.confidence(), match.snippet()))
                .toList();
    }

    /** One detected skill occurrence with evidence snippet and confidence. */
    public record Candidate(Skill skill, double confidence, String snippet) {
    }
}
