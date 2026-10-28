package com.skillgap.controller;

import com.skillgap.entity.InputDocument;
import com.skillgap.entity.SkillExtractionRun;
import com.skillgap.entity.User;
import com.skillgap.entity.enums.DocumentType;
import com.skillgap.entity.enums.ExtractionStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.DocumentService;
import com.skillgap.service.DocumentStorageService;
import com.skillgap.service.EvidenceService;
import com.skillgap.service.NotificationService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Resume and job-description uploads, plus review of extracted skills. */
@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final CurrentUserService currentUserService;
    private final DocumentService documentService;
    private final DocumentStorageService storageService;
    private final EvidenceService evidenceService;
    private final NotificationService notificationService;

    public DocumentController(CurrentUserService currentUserService,
                              DocumentService documentService,
                              DocumentStorageService storageService,
                              EvidenceService evidenceService,
                              NotificationService notificationService) {
        this.currentUserService = currentUserService;
        this.documentService = documentService;
        this.storageService = storageService;
        this.evidenceService = evidenceService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUserService.getUser();
        model.addAttribute("documents", documentService.listDocumentsForUser(user));
        model.addAttribute("runs", documentService.listRunsForUser(user));
        return "documents/list";
    }

    @PostMapping("/resume/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        return handleUpload(file, DocumentType.RESUME, redirectAttributes);
    }

    @PostMapping("/job-description/upload")
    public String uploadJobDescription(@RequestParam("file") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        return handleUpload(file, DocumentType.JOB_DESCRIPTION, redirectAttributes);
    }

    private String handleUpload(MultipartFile file, DocumentType type, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        try {
            SkillExtractionRun run = documentService.uploadAndExtract(user, file, type);
            if (run.getStatus() == ExtractionStatus.COMPLETED) {
                if (type == DocumentType.RESUME) {
                    notificationService.notify(user, "Resume processed", run.getMessage());
                }
                redirectAttributes.addFlashAttribute("message", run.getMessage());
                return "redirect:/documents/runs/" + run.getId();
            }
            redirectAttributes.addFlashAttribute("errorMessage", run.getMessage());
            return "redirect:/documents";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/documents";
        }
    }

    /** Review screen: confirm/reject each extracted skill. */
    @GetMapping("/runs/{runId}")
    public String reviewRun(@PathVariable Long runId, Model model) {
        User user = currentUserService.getUser();
        SkillExtractionRun run = documentService.listRunsForUser(user).stream()
                .filter(r -> r.getId().equals(runId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Extraction run not found"));
        model.addAttribute("run", run);
        model.addAttribute("evidence", evidenceService.listForUser(user, runId));
        model.addAttribute("document", run.getInputDocument());
        return "documents/review";
    }

    @PostMapping("/evidence/{evidenceId}/confirm")
    public String confirmEvidence(@PathVariable Long evidenceId,
                                  @RequestParam("level") int level,
                                  RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        Long runId = runIdOfEvidence(user, evidenceId);
        try {
            evidenceService.confirm(user, evidenceId, level, null);
            redirectAttributes.addFlashAttribute("message", "Skill confirmed and added to My Skills.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirect(runId);
    }

    @PostMapping("/evidence/{evidenceId}/reject")
    public String rejectEvidence(@PathVariable Long evidenceId, RedirectAttributes redirectAttributes) {
        User user = currentUserService.getUser();
        Long runId = runIdOfEvidence(user, evidenceId);
        try {
            evidenceService.reject(user, evidenceId);
            redirectAttributes.addFlashAttribute("message", "Marked as not one of your skills.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirect(runId);
    }

    private String redirect(Long runId) {
        return runId == null ? "redirect:/documents" : "redirect:/documents/runs/" + runId;
    }

    private Long runIdOfEvidence(User user, Long evidenceId) {
        var evidence = evidenceService.getOwned(user, evidenceId);
        return evidence.getExtractionRun() == null ? null : evidence.getExtractionRun().getId();
    }

    /** Secured download of the user's own uploaded file (ownership checked). */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        User user = currentUserService.getUser();
        InputDocument document = documentService.getOwnedDocument(user, id);
        if (document.getStoredPath() == null) {
            throw new BusinessException("This entry has no stored file.");
        }
        InputStreamResource resource = new InputStreamResource(storageService.open(document.getStoredPath()));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(resource);
    }
}
