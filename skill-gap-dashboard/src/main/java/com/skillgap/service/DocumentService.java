package com.skillgap.service;

import com.skillgap.entity.InputDocument;
import com.skillgap.entity.SkillEvidence;
import com.skillgap.entity.SkillExtractionRun;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.DocumentType;
import com.skillgap.entity.enums.ExtractionStatus;
import com.skillgap.entity.enums.SourceType;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.InputDocumentRepository;
import com.skillgap.repository.SkillEvidenceRepository;
import com.skillgap.repository.SkillExtractionRunRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates resume / job description uploads:
 * validate -> store privately -> extract text -> match catalog -> save PENDING
 * evidence for the user to review.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "docx");

    private final InputDocumentRepository inputDocumentRepository;
    private final SkillExtractionRunRepository runRepository;
    private final SkillEvidenceRepository evidenceRepository;
    private final DocumentStorageService storageService;
    private final DocumentExtractionService extractionService;
    private final SkillExtractionService skillExtractionService;

    public DocumentService(InputDocumentRepository inputDocumentRepository,
                           SkillExtractionRunRepository runRepository,
                           SkillEvidenceRepository evidenceRepository,
                           DocumentStorageService storageService,
                           DocumentExtractionService extractionService,
                           SkillExtractionService skillExtractionService) {
        this.inputDocumentRepository = inputDocumentRepository;
        this.runRepository = runRepository;
        this.evidenceRepository = evidenceRepository;
        this.storageService = storageService;
        this.extractionService = extractionService;
        this.skillExtractionService = skillExtractionService;
    }

    /** Uploads a resume or job description and runs skill extraction on it. */
    @Transactional
    public SkillExtractionRun uploadAndExtract(User user, MultipartFile file, DocumentType documentType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please choose a file to upload.");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Unsupported file type ." + extension
                    + ". Only PDF and DOCX files are accepted.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("File is too large. Maximum size is 10 MB.");
        }

        // 1) store the raw file in private storage
        String storedPath = storageService.store(user, file, extension);

        InputDocument document = new InputDocument(user, documentType);
        document.setOriginalFilename(sanitize(file.getOriginalFilename()));
        document.setStoredPath(storedPath);
        document.setContentType(file.getContentType());
        document.setFileSizeBytes(file.getSize());
        document.setDocumentType(documentType);
        document.setTitle(documentType == DocumentType.RESUME ? "Resume upload" : "Job description upload");
        document.setUploadedAt(Instant.now());
        inputDocumentRepository.save(document);

        return runExtraction(user, document);
    }

    /** Runs (or re-runs) extraction over a stored document's text. */
    @Transactional
    public SkillExtractionRun runExtraction(User user, InputDocument document) {
        SkillExtractionRun run = new SkillExtractionRun(user, document);
        run.setRanAt(Instant.now());
        // Persist the run first so its evidence rows can reference it (IDENTITY ids).
        run = runRepository.save(run);

        try {
            byte[] bytes;
            try (var in = storageService.open(document.getStoredPath())) {
                bytes = in.readAllBytes();
            }
            String extension = extensionOf(document.getOriginalFilename());
            String text = extractionService.extractText(bytes, extension);

            if (extractionService.isBlank(text)) {
                document.setStatus(ExtractionStatus.FAILED);
                document.setStatusMessage("No readable text found. The file may be a scanned image; "
                        + "OCR is not supported in this version.");
                run.setStatus(ExtractionStatus.FAILED);
                run.setMessage(document.getStatusMessage());
                return runRepository.save(run);
            }

            document.setExtractedText(text);
            document.setStatus(ExtractionStatus.COMPLETED);
            document.setStatusMessage(null);

            List<SkillExtractionService.Candidate> candidates = skillExtractionService.extractCandidates(text);
            SourceType sourceType = switch (document.getDocumentType()) {
                case RESUME -> SourceType.RESUME;
                case JOB_DESCRIPTION -> SourceType.JOB_DESCRIPTION;
                case WORK_SPECIFICATION -> SourceType.WORK_SPECIFICATION;
            };
            for (SkillExtractionService.Candidate candidate : candidates) {
                SkillEvidence evidence = new SkillEvidence(user, candidate.skill(), sourceType);
                evidence.setExtractionRun(run);
                evidence.setContextSnippet(candidate.snippet());
                evidence.setConfidence(candidate.confidence());
                evidence.setVerificationStatus(VerificationStatus.PENDING);
                evidence.setRecordedAt(Instant.now());
                evidenceRepository.save(evidence);
            }

            run.setStatus(ExtractionStatus.COMPLETED);
            run.setCandidateCount(candidates.size());
            run.setMessage(candidates.isEmpty()
                    ? "No catalog skills were detected. You can add skills manually."
                    : "Detected " + candidates.size() + " skill(s). Please review them below.");
            inputDocumentRepository.save(document);
            return runRepository.save(run);
        } catch (BusinessException e) {
            document.setStatus(ExtractionStatus.FAILED);
            document.setStatusMessage(e.getMessage());
            run.setStatus(ExtractionStatus.FAILED);
            run.setMessage(e.getMessage());
            return runRepository.save(run);
        } catch (Exception e) {
            log.error("Extraction failed for document {}", document.getId(), e);
            String message = "Extraction failed because of an unexpected error. Please try a different file.";
            document.setStatus(ExtractionStatus.FAILED);
            document.setStatusMessage(message);
            run.setStatus(ExtractionStatus.FAILED);
            run.setMessage(message);
            return runRepository.save(run);
        }
    }

    // ------------------------------------------------------------------- helpers

    public InputDocument getOwnedDocument(User user, Long id) {
        return inputDocumentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    public List<InputDocument> listDocumentsForUser(User user) {
        return inputDocumentRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
    }

    public List<SkillExtractionRun> listRunsForUser(User user) {
        return runRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<SkillEvidence> evidenceForRun(Long runId) {
        return evidenceRepository.findByExtractionRunIdOrderBySkillNameAsc(runId);
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase().trim();
    }

    private String sanitize(String filename) {
        if (filename == null) {
            return "upload";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }
}
