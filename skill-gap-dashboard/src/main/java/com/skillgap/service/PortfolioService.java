package com.skillgap.service;

import com.skillgap.dto.CertificationForm;
import com.skillgap.dto.ProjectForm;
import com.skillgap.entity.Certification;
import com.skillgap.entity.Skill;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProject;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.CertificationRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.repository.UserProjectRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Certifications and portfolio projects - stored as supporting evidence. */
@Service
public class PortfolioService {

    private final CertificationRepository certificationRepository;
    private final UserProjectRepository projectRepository;
    private final SkillRepository skillRepository;

    public PortfolioService(CertificationRepository certificationRepository,
                            UserProjectRepository projectRepository,
                            SkillRepository skillRepository) {
        this.certificationRepository = certificationRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
    }

    // ----------------------------------------------------------- certifications

    @Transactional(readOnly = true)
    public List<Certification> listCertifications(User user) {
        return certificationRepository.findByUserIdOrderByIssueDateDesc(user.getId());
    }

    @Transactional
    public Certification addCertification(User user, CertificationForm form) {
        if (form.getName() == null || form.getName().isBlank()
                || form.getOrganization() == null || form.getOrganization().isBlank()) {
            throw new BusinessException("Certification name and organization are required.");
        }
        Certification certification = new Certification(user, form.getName().trim(), form.getOrganization().trim());
        certification.setIssueDate(form.getIssueDate());
        certification.setCredentialUrl(trimToNull(form.getCredentialUrl()));
        if (form.getSkillId() != null && form.getSkillId() > 0) {
            Skill skill = skillRepository.findById(form.getSkillId()).orElse(null);
            certification.setSkill(skill);
        }
        return certificationRepository.save(certification);
    }

    @Transactional
    public void deleteCertification(User user, Long id) {
        Certification certification = certificationRepository.findById(id)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Certification not found"));
        certificationRepository.delete(certification);
    }

    // ----------------------------------------------------------------- projects

    @Transactional(readOnly = true)
    public List<UserProject> listProjects(User user) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public UserProject addProject(User user, ProjectForm form) {
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            throw new BusinessException("Project title is required.");
        }
        UserProject project = new UserProject(user, form.getTitle().trim());
        project.setDescription(trimToNull(form.getDescription()));
        project.setTechnologies(trimToNull(form.getTechnologies()));
        project.setProjectUrl(trimToNull(form.getProjectUrl()));
        LocalDate date = form.getCompletedDate();
        project.setCompletedDate(date);
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(User user, Long id) {
        UserProject project = projectRepository.findById(id)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Project not found"));
        projectRepository.delete(project);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
