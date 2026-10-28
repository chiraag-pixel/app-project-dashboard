package com.skillgap.repository;

import com.skillgap.entity.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNormalizedName(String normalizedName);

    List<Skill> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<Skill> findByNameContainingIgnoreCase(String namePart);

    @Query("select s from Skill s order by s.name")
    List<Skill> findAllOrdered();
}
