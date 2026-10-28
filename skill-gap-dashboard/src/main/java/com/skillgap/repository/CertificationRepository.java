package com.skillgap.repository;

import com.skillgap.entity.Certification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    List<Certification> findByUserIdOrderByIssueDateDesc(Long userId);

    List<Certification> findByUserId(Long userId);
}
