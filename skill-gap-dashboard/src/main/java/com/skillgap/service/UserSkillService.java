package com.skillgap.service;

import com.skillgap.dto.UserSkillForm;
import com.skillgap.entity.Skill;
import com.skillgap.entity.SkillCategory;
import com.skillgap.entity.User;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.SourceType;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.SkillCategoryRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.repository.UserSkillRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD for the current user's recorded skills. */
@Service
public class UserSkillService {

    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final SkillCatalogService skillCatalogService;

    public UserSkillService(UserSkillRepository userSkillRepository, SkillRepository skillRepository,
                            SkillCategoryRepository categoryRepository, SkillCatalogService skillCatalogService) {
        this.userSkillRepository = userSkillRepository;
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.skillCatalogService = skillCatalogService;
    }

    @Transactional(readOnly = true)
    public List<UserSkill> listForUser(User user) {
        return userSkillRepository.findWithSkillByUserId(user.getId());
    }

    /** Adds a skill (from catalog id or a brand-new name) at a given level. */
    @Transactional
    public UserSkill addSkill(User user, UserSkillForm form) {
        Skill skill = resolveSkill(form);
        return userSkillRepository.findByUserIdAndSkillId(user.getId(), skill.getId())
                .map(existing -> update(existing, form))
                .orElseGet(() -> {
                    UserSkill userSkill = new UserSkill(user, skill, form.getLevel());
                    applyDetails(userSkill, form);
                    return userSkillRepository.save(userSkill);
                });
    }

    @Transactional
    public UserSkill updateSkill(User user, Long userSkillId, UserSkillForm form) {
        UserSkill userSkill = getOwned(user, userSkillId);
        userSkill.setLevel(form.getLevel());
        applyDetails(userSkill, form);
        return userSkillRepository.save(userSkill);
    }

    @Transactional
    public void deleteSkill(User user, Long userSkillId) {
        UserSkill userSkill = getOwned(user, userSkillId);
        userSkillRepository.delete(userSkill);
    }

    @Transactional(readOnly = true)
    public UserSkill getOwned(User user, Long id) {
        return userSkillRepository.findById(id)
                .filter(us -> us.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Skill not found in your profile"));
    }

    /**
     * Upserts a user skill at a given level, recording the source (used by
     * confirmed extraction results and assessments).
     */
    @Transactional
    public UserSkill upsert(User user, Skill skill, int level, SourceType sourceType, String notes) {
        UserSkill userSkill = userSkillRepository.findByUserIdAndSkillId(user.getId(), skill.getId())
                .orElseGet(() -> new UserSkill(user, skill, level));
        // Never downgrade silently; the user can edit manually if they disagree.
        if (userSkill.getLevel() == null || level > userSkill.getLevel()) {
            userSkill.setLevel(level);
        }
        if (userSkill.getSourceType() == null || userSkill.getSourceType() == SourceType.MANUAL) {
            userSkill.setSourceType(sourceType);
        }
        if (notes != null && !notes.isBlank()) {
            userSkill.setNotes(truncate(userSkill.getNotes(), notes));
        }
        if (sourceType == SourceType.ASSESSMENT) {
            userSkill.setLastAssessedAt(Instant.now());
        }
        return userSkillRepository.save(userSkill);
    }

    private UserSkill update(UserSkill existing, UserSkillForm form) {
        existing.setLevel(form.getLevel());
        applyDetails(existing, form);
        return userSkillRepository.save(existing);
    }

    private void applyDetails(UserSkill userSkill, UserSkillForm form) {
        userSkill.setYearsOfExperience(form.getYearsOfExperience());
        userSkill.setNotes(form.getNotes() == null || form.getNotes().isBlank() ? null : form.getNotes().trim());
        if (form.getSkillId() == null || form.getSkillId() <= 0) {
            userSkill.setSourceType(SourceType.MANUAL);
        }
    }

    private Skill resolveSkill(UserSkillForm form) {
        if (form.getSkillId() != null && form.getSkillId() > 0) {
            return skillRepository.findById(form.getSkillId())
                    .orElseThrow(() -> new NotFoundException("Selected skill does not exist"));
        }
        if (form.getNewSkillName() == null || form.getNewSkillName().isBlank()) {
            throw new BusinessException("Choose a skill from the catalog or type a new skill name.");
        }
        // New skills are created in the shared catalog under "Other".
        SkillCategory other = categoryRepository.findByNameIgnoreCase("Other").orElse(null);
        return skillCatalogService.findOrCreate(form.getNewSkillName(), other, new boolean[1]);
    }

    private String truncate(String existingNotes, String newNotes) {
        if (existingNotes == null || existingNotes.isBlank()) {
            return newNotes;
        }
        return existingNotes + " | " + newNotes;
    }
}
