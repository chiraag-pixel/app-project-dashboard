package com.skillgap.repository;

import com.skillgap.entity.SkillCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    Optional<SkillCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
