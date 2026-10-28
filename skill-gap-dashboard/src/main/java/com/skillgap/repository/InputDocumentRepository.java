package com.skillgap.repository;

import com.skillgap.entity.InputDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InputDocumentRepository extends JpaRepository<InputDocument, Long> {

    List<InputDocument> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<InputDocument> findByIdAndUserId(Long id, Long userId);

    List<InputDocument> findByUserIdAndDocumentTypeOrderByUploadedAtDesc(Long userId,
            com.skillgap.entity.enums.DocumentType documentType);
}
