package com.skillgap.service;

import com.skillgap.entity.JobRole;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.JobRoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only access to predefined job roles for regular users. */
@Service
public class JobRoleQueryService {

    private final JobRoleRepository jobRoleRepository;

    public JobRoleQueryService(JobRoleRepository jobRoleRepository) {
        this.jobRoleRepository = jobRoleRepository;
    }

    @Transactional(readOnly = true)
    public List<JobRole> listRoles() {
        return jobRoleRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public JobRole getRole(Long id) {
        return jobRoleRepository.findById(id).orElseThrow(() -> new NotFoundException("Job role not found"));
    }
}
