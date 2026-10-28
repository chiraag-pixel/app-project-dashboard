package com.skillgap.repository;

import com.skillgap.entity.UserProject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    List<UserProject> findByUserIdOrderByCreatedAtDesc(Long userId);
}
