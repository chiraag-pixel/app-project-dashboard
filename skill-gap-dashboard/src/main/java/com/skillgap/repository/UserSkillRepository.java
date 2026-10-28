package com.skillgap.repository;

import com.skillgap.entity.UserSkill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByUserIdOrderBySkillNameAsc(Long userId);

    Optional<UserSkill> findByUserIdAndSkillId(Long userId, Long skillId);

    long countByUserId(Long userId);

    @Query("select us from UserSkill us join fetch us.skill where us.user.id = :userId")
    List<UserSkill> findWithSkillByUserId(@Param("userId") Long userId);
}
