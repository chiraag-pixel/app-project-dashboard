package com.skillgap.service;

import com.skillgap.entity.Assessment;
import com.skillgap.entity.AssessmentOption;
import com.skillgap.entity.AssessmentQuestion;
import com.skillgap.entity.JobRole;
import com.skillgap.entity.JobRoleSkill;
import com.skillgap.entity.LearningResource;
import com.skillgap.entity.Skill;
import com.skillgap.entity.SkillCategory;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.Role;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.AssessmentRepository;
import com.skillgap.repository.JobRoleRepository;
import com.skillgap.repository.LearningResourceRepository;
import com.skillgap.repository.SkillCategoryRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.repository.UserRepository;
import com.skillgap.util.SkillNormalizer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All administrative operations, with audit logging of changes. */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final JobRoleRepository jobRoleRepository;
    private final AssessmentRepository assessmentRepository;
    private final LearningResourceRepository resourceRepository;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository,
                        SkillCategoryRepository categoryRepository,
                        SkillRepository skillRepository,
                        JobRoleRepository jobRoleRepository,
                        AssessmentRepository assessmentRepository,
                        LearningResourceRepository resourceRepository,
                        AuditService auditService) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.assessmentRepository = assessmentRepository;
        this.resourceRepository = resourceRepository;
        this.auditService = auditService;
    }

    // --------------------------------------------------------------------- users

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void updateUser(User admin, Long userId, Role role, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getId().equals(admin.getId()) && (role != Role.ADMIN || !enabled)) {
            throw new BusinessException("You cannot remove your own admin access here.");
        }
        user.setRole(role);
        user.setEnabled(enabled);
        userRepository.save(user);
        auditService.record(admin, "USER_UPDATED", "User", userId,
                "role=" + role + ", enabled=" + enabled + ", email=" + user.getEmail());
    }

    // ---------------------------------------------------------------- categories

    @Transactional(readOnly = true)
    public List<SkillCategory> listCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public SkillCategory createCategory(User admin, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Category name is required.");
        }
        if (categoryRepository.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessException("A category with this name already exists.");
        }
        SkillCategory category = categoryRepository.save(new SkillCategory(name.trim(), trimToNull(description)));
        auditService.record(admin, "CATEGORY_CREATED", "SkillCategory", category.getId(), category.getName());
        return category;
    }

    @Transactional
    public void deleteCategory(User admin, Long categoryId) {
        SkillCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (!skillRepository.findByCategoryIdOrderByNameAsc(categoryId).isEmpty()) {
            throw new BusinessException("Move or delete the skills in this category first.");
        }
        auditService.record(admin, "CATEGORY_DELETED", "SkillCategory", categoryId, category.getName());
        categoryRepository.delete(category);
    }

    // -------------------------------------------------------------------- skills

    @Transactional(readOnly = true)
    public List<Skill> listSkills() {
        return skillRepository.findAllOrdered();
    }

    @Transactional
    public Skill createSkill(User admin, String name, Long categoryId, String description, String aliases) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Skill name is required.");
        }
        String normalized = SkillNormalizer.normalize(name);
        if (skillRepository.findByNormalizedName(normalized).isPresent()) {
            throw new BusinessException("A skill with this name already exists.");
        }
        SkillCategory category = categoryId == null ? null : categoryRepository.findById(categoryId).orElse(null);
        Skill skill = new Skill(name.trim(), normalized, category);
        skill.setDescription(trimToNull(description));
        skill.setAliases(trimToNull(aliases));
        skillRepository.save(skill);
        auditService.record(admin, "SKILL_CREATED", "Skill", skill.getId(), skill.getName());
        return skill;
    }

    @Transactional
    public Skill updateSkill(User admin, Long skillId, String name, Long categoryId, String description, String aliases) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found"));
        if (name != null && !name.isBlank()) {
            skill.setName(name.trim());
            skill.setNormalizedName(SkillNormalizer.normalize(name));
        }
        skill.setCategory(categoryId == null ? null : categoryRepository.findById(categoryId).orElse(null));
        skill.setDescription(trimToNull(description));
        skill.setAliases(trimToNull(aliases));
        skillRepository.save(skill);
        auditService.record(admin, "SKILL_UPDATED", "Skill", skill.getId(), skill.getName());
        return skill;
    }

    @Transactional
    public void deleteSkill(User admin, Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found"));
        auditService.record(admin, "SKILL_DELETED", "Skill", skillId, skill.getName());
        skillRepository.delete(skill);
    }

    // ---------------------------------------------------------------- job roles

    @Transactional(readOnly = true)
    public List<JobRole> listRoles() {
        return jobRoleRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public JobRole getRole(Long id) {
        return jobRoleRepository.findById(id).orElseThrow(() -> new NotFoundException("Job role not found"));
    }

    @Transactional
    public JobRole createRole(User admin, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Role name is required.");
        }
        if (jobRoleRepository.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessException("A job role with this name already exists.");
        }
        JobRole role = jobRoleRepository.save(new JobRole(name.trim(), trimToNull(description)));
        auditService.record(admin, "ROLE_CREATED", "JobRole", role.getId(), role.getName());
        return role;
    }

    @Transactional
    public void updateRole(User admin, Long roleId, String name, String description) {
        JobRole role = getRole(roleId);
        if (name != null && !name.isBlank()) {
            role.setName(name.trim());
        }
        role.setDescription(trimToNull(description));
        jobRoleRepository.save(role);
        auditService.record(admin, "ROLE_UPDATED", "JobRole", roleId, role.getName());
    }

    @Transactional
    public void setRoleSkill(User admin, Long roleId, Long skillId, int requiredLevel, int weight,
                              com.skillgap.entity.enums.RequirementType type) {
        JobRole role = getRole(roleId);
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found"));
        if (requiredLevel < 1 || requiredLevel > 5) {
            throw new BusinessException("Required level must be 1-5.");
        }
        if (weight < 1 || weight > 5) {
            throw new BusinessException("Weight must be 1-5.");
        }
        role.getSkills().removeIf(rs -> rs.getSkill().getId().equals(skillId));
        role.addSkill(new JobRoleSkill(skill, requiredLevel, weight, type));
        jobRoleRepository.save(role);
        auditService.record(admin, "ROLE_SKILL_SET", "JobRole", roleId,
                role.getName() + " <- " + skill.getName() + " level " + requiredLevel + " weight " + weight);
    }

    @Transactional
    public void removeRoleSkill(User admin, Long roleId, Long skillId) {
        JobRole role = getRole(roleId);
        role.getSkills().removeIf(rs -> rs.getSkill().getId().equals(skillId));
        jobRoleRepository.save(role);
        auditService.record(admin, "ROLE_SKILL_REMOVED", "JobRole", roleId, "skillId=" + skillId);
    }

    @Transactional
    public void deleteRole(User admin, Long roleId) {
        JobRole role = getRole(roleId);
        auditService.record(admin, "ROLE_DELETED", "JobRole", roleId, role.getName());
        jobRoleRepository.delete(role);
    }

    // ---------------------------------------------------------------- assessments

    @Transactional(readOnly = true)
    public List<Assessment> listAssessments() {
        return assessmentRepository.findAllByOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public Assessment getAssessment(Long id) {
        return assessmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Assessment not found"));
    }

    @Transactional
    public Assessment createAssessment(User admin, String title, String description, Long skillId,
                                       int passingScore, boolean active) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("Assessment title is required.");
        }
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found"));
        Assessment assessment = new Assessment(title.trim(), skill, passingScore);
        assessment.setDescription(trimToNull(description));
        assessment.setActive(active);
        assessmentRepository.save(assessment);
        auditService.record(admin, "ASSESSMENT_CREATED", "Assessment", assessment.getId(), title);
        return assessment;
    }

    @Transactional
    public void setAssessmentActive(User admin, Long assessmentId, boolean active) {
        Assessment assessment = getAssessment(assessmentId);
        assessment.setActive(active);
        assessmentRepository.save(assessment);
        auditService.record(admin, "ASSESSMENT_STATUS", "Assessment", assessmentId, "active=" + active);
    }

    @Transactional
    public AssessmentQuestion addQuestion(User admin, Long assessmentId, String questionText,
                                          List<String> options, int correctIndex) {
        Assessment assessment = getAssessment(assessmentId);
        if (questionText == null || questionText.isBlank()) {
            throw new BusinessException("Question text is required.");
        }
        if (options == null || options.size() < 2 || options.stream().allMatch(o -> o == null || o.isBlank())) {
            throw new BusinessException("Provide at least two answer options.");
        }
        if (correctIndex < 0 || correctIndex >= options.size()) {
            throw new BusinessException("Choose the correct option.");
        }
        long orderNo = assessment.getQuestions().size() + 1;
        AssessmentQuestion question = new AssessmentQuestion(assessment, questionText.trim(), (int) orderNo);
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i) == null || options.get(i).isBlank()) {
                continue;
            }
            question.addOption(new AssessmentOption(question, options.get(i).trim(), i == correctIndex));
        }
        assessment.getQuestions().add(question);
        assessmentRepository.save(assessment);
        auditService.record(admin, "QUESTION_ADDED", "Assessment", assessmentId, questionText);
        return question;
    }

    // --------------------------------------------------------- learning resources

    @Transactional(readOnly = true)
    public List<LearningResource> listResources() {
        return resourceRepository.findAllByOrderByTitleAsc();
    }

    @Transactional
    public LearningResource createResource(User admin, String title, String url, Long skillId,
                                           int difficulty, Double estimatedHours, String description) {
        if (title == null || title.isBlank() || url == null || url.isBlank()) {
            throw new BusinessException("Title and URL are required.");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BusinessException("Resource URL must start with http:// or https://");
        }
        Skill skill = skillId == null ? null : skillRepository.findById(skillId).orElse(null);
        LearningResource resource = new LearningResource(title.trim(), url.trim(), skill);
        resource.setDifficulty(difficulty);
        resource.setEstimatedHours(estimatedHours);
        resource.setDescription(trimToNull(description));
        resourceRepository.save(resource);
        auditService.record(admin, "RESOURCE_CREATED", "LearningResource", resource.getId(), title);
        return resource;
    }

    @Transactional
    public void setResourceActive(User admin, Long resourceId, boolean active) {
        LearningResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        resource.setActive(active);
        resourceRepository.save(resource);
        auditService.record(admin, "RESOURCE_STATUS", "LearningResource", resourceId, "active=" + active);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
