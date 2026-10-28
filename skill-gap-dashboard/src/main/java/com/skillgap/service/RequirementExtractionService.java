package com.skillgap.service;

import com.skillgap.entity.AnalysisRequirement;
import com.skillgap.entity.InputDocument;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.RequirementType;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.repository.AnalysisRequirementRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule-based requirement extraction for typed work specifications and custom
 * job descriptions. Deterministic heuristics infer importance and level from
 * the surrounding sentence; users review every suggestion before it is used.
 */
@Service
public class RequirementExtractionService {

    private final SkillCatalogService skillCatalogService;
    private final AnalysisRequirementRepository requirementRepository;

    public RequirementExtractionService(SkillCatalogService skillCatalogService,
                                        AnalysisRequirementRepository requirementRepository) {
        this.skillCatalogService = skillCatalogService;
        this.requirementRepository = requirementRepository;
    }

    /** Extracts PENDING requirements into the given document and saves them. */
    @Transactional
    public List<AnalysisRequirement> extractInto(User user, InputDocument document, String text) {
        List<AnalysisRequirement> saved = new java.util.ArrayList<>();
        for (SkillCatalogService.TermMatch match : skillCatalogService.matchTerms(text)) {
            String context = match.snippet().toLowerCase(Locale.ROOT);
            AnalysisRequirement requirement = new AnalysisRequirement(user, document, match.skill(), inferLevel(context));
            requirement.setWeight(inferWeight(context));
            requirement.setRequirementType(inferType(context));
            requirement.setVerificationStatus(VerificationStatus.PENDING);
            requirement.setContextSnippet(match.snippet());
            saved.add(requirementRepository.save(requirement));
        }
        return saved;
    }

    // "expert"/"deep experience" implies level 5, "advanced" 4, "basic" 2, else 3.
    int inferLevel(String context) {
        if (context.contains("expert") || context.contains("deep knowledge") || context.contains("deep understanding")) {
            return 5;
        }
        if (context.contains("advanced")) {
            return 4;
        }
        if (context.contains("basic") || context.contains("familiarity")) {
            return 2;
        }
        return 3;
    }

    RequirementType inferType(String context) {
        if (context.contains("nice to have") || context.contains("preferred") || context.contains("bonus")
                || context.contains("optional") || context.contains("would be great")) {
            return RequirementType.PREFERRED;
        }
        return RequirementType.REQUIRED;
    }

    int inferWeight(String context) {
        if (context.contains("must") || context.contains("required") || context.contains("essential")
                || context.contains("critical") || context.contains("strong")) {
            return 4;
        }
        if (context.contains("nice to have") || context.contains("preferred") || context.contains("bonus")) {
            return 2;
        }
        return 3;
    }
}
