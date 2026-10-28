package com.skillgap.service;

import com.skillgap.dto.WorkSpecForm;
import com.skillgap.entity.AnalysisRequirement;
import com.skillgap.entity.InputDocument;
import com.skillgap.entity.Skill;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.DocumentType;
import com.skillgap.entity.enums.ExtractionStatus;
import com.skillgap.entity.enums.RequirementType;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.AnalysisRequirementRepository;
import com.skillgap.repository.InputDocumentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Work specifications and custom job descriptions are stored as text-only
 * InputDocuments; their extracted skill requirements are reviewed by the user
 * before any analysis runs against them.
 */
@Service
public class WorkSpecService {

    private final InputDocumentRepository inputDocumentRepository;
    private final AnalysisRequirementRepository requirementRepository;
    private final RequirementExtractionService requirementExtractionService;
    private final SkillCatalogService skillCatalogService;

    public WorkSpecService(InputDocumentRepository inputDocumentRepository,
                           AnalysisRequirementRepository requirementRepository,
                           RequirementExtractionService requirementExtractionService,
                           SkillCatalogService skillCatalogService) {
        this.inputDocumentRepository = inputDocumentRepository;
        this.requirementRepository = requirementRepository;
        this.requirementExtractionService = requirementExtractionService;
        this.skillCatalogService = skillCatalogService;
    }

    @Transactional
    public InputDocument createWorkSpec(User user, WorkSpecForm form) {
        return createFromText(user, DocumentType.WORK_SPECIFICATION, form.getTitle().trim(), form.getDescription());
    }

    @Transactional
    public InputDocument createCustomJobDescription(User user, String title, String description) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("Please give the job description a title.");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("Please paste the job description text.");
        }
        return createFromText(user, DocumentType.JOB_DESCRIPTION, title.trim(), description.trim());
    }

    private InputDocument createFromText(User user, DocumentType type, String title, String text) {
        InputDocument document = new InputDocument(user, type);
        document.setTitle(title);
        document.setExtractedText(text);
        document.setStatus(ExtractionStatus.COMPLETED);
        document.setUploadedAt(Instant.now());
        inputDocumentRepository.save(document);

        requirementExtractionService.extractInto(user, document, text);
        return document;
    }

    // ----------------------------------------------------------------- documents

    public List<InputDocument> listForUser(User user) {
        return inputDocumentRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
    }

    public List<InputDocument> listWorkSpecs(User user) {
        return inputDocumentRepository.findByUserIdAndDocumentTypeOrderByUploadedAtDesc(
                user.getId(), DocumentType.WORK_SPECIFICATION);
    }

    public InputDocument getOwned(User user, Long id) {
        return inputDocumentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Specification not found"));
    }

    @Transactional
    public void delete(User user, Long id) {
        InputDocument document = getOwned(user, id);
        requirementRepository.deleteByInputDocumentId(document.getId());
        inputDocumentRepository.delete(document);
    }

    // -------------------------------------------------------------- requirements

    public List<AnalysisRequirement> listRequirements(User user, Long documentId) {
        getOwned(user, documentId);
        return requirementRepository.findByUserIdAndInputDocumentIdOrderBySkillNameAsc(user.getId(), documentId);
    }

    /** Requirements the user confirmed - these drive the analysis. */
    public List<AnalysisRequirement> confirmedRequirements(User user, Long documentId) {
        return requirementRepository.findByUserIdAndInputDocumentIdAndVerificationStatus(
                user.getId(), documentId, VerificationStatus.CONFIRMED);
    }

    public AnalysisRequirement getOwnedRequirement(User user, Long requirementId) {
        return requirementRepository.findById(requirementId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Requirement not found"));
    }

    @Transactional
    public AnalysisRequirement updateRequirement(User user, Long requirementId, Integer level, Integer weight,
                                                 RequirementType type, VerificationStatus status) {
        AnalysisRequirement requirement = getOwnedRequirement(user, requirementId);
        if (level != null) {
            if (level < 1 || level > 5) {
                throw new BusinessException("Level must be between 1 and 5.");
            }
            requirement.setRequiredLevel(level);
        }
        if (weight != null) {
            if (weight < 1 || weight > 5) {
                throw new BusinessException("Weight must be between 1 and 5.");
            }
            requirement.setWeight(weight);
        }
        if (type != null) {
            requirement.setRequirementType(type);
        }
        if (status != null) {
            requirement.setVerificationStatus(status);
        }
        return requirementRepository.save(requirement);
    }

    @Transactional
    public AnalysisRequirement addRequirement(User user, Long documentId, Long skillId, String newSkillName,
                                              Integer level, Integer weight, RequirementType type) {
        InputDocument document = getOwned(user, documentId);
        Skill skill;
        if (skillId != null && skillId > 0) {
            skill = skillCatalogService.getSkill(skillId);
        } else if (newSkillName != null && !newSkillName.isBlank()) {
            skill = skillCatalogService.findOrCreate(newSkillName, null, new boolean[1]);
        } else {
            throw new BusinessException("Choose or type a skill for the requirement.");
        }
        AnalysisRequirement requirement = new AnalysisRequirement(user, document, skill, level);
        requirement.setWeight(weight == null ? 3 : weight);
        requirement.setRequirementType(type == null ? RequirementType.REQUIRED : type);
        requirement.setVerificationStatus(VerificationStatus.CONFIRMED);
        return requirementRepository.save(requirement);
    }
}
