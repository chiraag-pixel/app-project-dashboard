package com.skillgap.repository;

import com.skillgap.entity.JobRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRoleRepository extends JpaRepository<JobRole, Long> {

    Optional<JobRole> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<JobRole> findByNameContainingIgnoreCaseOrderByNameAsc(String namePart);

    List<JobRole> findAllByOrderByNameAsc();
}
