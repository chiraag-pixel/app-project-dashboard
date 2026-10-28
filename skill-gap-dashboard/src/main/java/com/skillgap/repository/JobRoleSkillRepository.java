package com.skillgap.repository;

import com.skillgap.entity.JobRoleSkill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRoleSkillRepository extends JpaRepository<JobRoleSkill, Long> {

    List<JobRoleSkill> findByJobRoleId(Long jobRoleId);

    long countByJobRoleId(Long jobRoleId);
}
