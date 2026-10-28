package com.skillgap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skillgap.entity.Skill;
import com.skillgap.entity.SkillCategory;
import com.skillgap.entity.SkillEvidence;
import com.skillgap.entity.SkillExtractionRun;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.DocumentType;
import com.skillgap.entity.enums.ExtractionStatus;
import com.skillgap.entity.enums.Role;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.repository.SkillCategoryRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.repository.UserProfileRepository;
import com.skillgap.repository.UserRepository;
import com.skillgap.repository.UserSkillRepository;
import com.skillgap.service.DocumentExtractionService;
import com.skillgap.service.DocumentService;
import com.skillgap.service.EvidenceService;
import com.skillgap.util.SkillNormalizer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Document pipeline tests: PDF/DOCX text extraction, invalid/scanned file
 * handling, catalog matching, and the confirm/reject review flow.
 * OCR is explicitly not supported - scanned documents must fail gracefully.
 */
@SpringBootTest
class DocumentExtractionTest {

    @Autowired
    private DocumentExtractionService extractionService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private EvidenceService evidenceService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserSkillRepository userSkillRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private SkillCategoryRepository categoryRepository;

    private User user;
    private String tag;
    private Skill kotlinSkill;
    private Skill scalaSkill;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(
                new User("Doc Tester " + tag, "doc-" + tag + "@test.com", "unused-hash", Role.USER));
        userProfileRepository.save(new UserProfile(user));
        // Test-owned catalog skills so results do not depend on seed data.
        SkillCategory category = categoryRepository.save(new SkillCategory("Doc Cat " + tag, "test category"));
        kotlinSkill = skillRepository.save(new Skill("Kotlinex" + tag, SkillNormalizer.normalize("Kotlinex" + tag), category));
        scalaSkill = skillRepository.save(new Skill("Scalax" + tag, SkillNormalizer.normalize("Scalax" + tag), category));
    }

    private byte[] pdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
                content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                content.beginText();
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] blankPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());       // image-only style page: no text content
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] docxWithText(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(out);
            return out.toByteArray();
        }
    }

    // ------------------------------------------------------------- extraction

    @Test
    void extractsReadableTextFromPdf() throws Exception {
        byte[] pdf = pdfWithText("Resume of Jane Doe. Skills include " + kotlinSkill.getName() + ".");
        String text = extractionService.extractText(pdf, "pdf");
        assertThat(text).contains("Jane Doe").contains(kotlinSkill.getName());
    }

    @Test
    void extractsReadableTextFromDocx() throws Exception {
        byte[] docx = docxWithText("Experience with " + kotlinSkill.getName() + " and " + scalaSkill.getName() + ".");
        String text = extractionService.extractText(docx, "docx");
        assertThat(text).contains(kotlinSkill.getName()).contains(scalaSkill.getName());
    }

    @Test
    void corruptPdfProducesFriendlyErrorNotStackTrace() {
        byte[] garbage = "this is definitely not a pdf".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> extractionService.extractText(garbage, "pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("could not be read");
    }

    @Test
    void unsupportedExtensionIsRejectedBeforeAnyProcessing() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello world".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> documentService.uploadAndExtract(user, file, DocumentType.RESUME))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only PDF and DOCX");
    }

    @Test
    void emptyFileIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> documentService.uploadAndExtract(user, file, DocumentType.RESUME))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("choose a file");
    }

    @Test
    void scannedOrEmptyPdfFailsGracefullyWithOcrNotice() throws Exception {
        byte[] scanned = blankPdf();              // image-like: no extractable text
        MockMultipartFile file = new MockMultipartFile("file", "scanned.pdf", "application/pdf", scanned);
        SkillExtractionRun run = documentService.uploadAndExtract(user, file, DocumentType.RESUME);

        assertThat(run.getStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(run.getMessage()).containsIgnoringCase("OCR");
        assertThat(run.getInputDocument().getStatus()).isEqualTo(ExtractionStatus.FAILED);
    }

    // ---------------------------------------------------------- full pipeline

    @Test
    void resumeUploadDetectsCatalogSkillsAndConfirmationSavesThemWithEvidence() throws Exception {
        byte[] pdf = pdfWithText("Skilled " + kotlinSkill.getName() + " developer. Also used "
                + scalaSkill.getName() + " in two college projects.");
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf);

        SkillExtractionRun run = documentService.uploadAndExtract(user, file, DocumentType.RESUME);
        assertThat(run.getStatus()).isEqualTo(ExtractionStatus.COMPLETED);

        List<SkillEvidence> evidence = evidenceService.listForUser(user, run.getId());
        List<String> names = evidence.stream().map(e -> e.getSkill().getName()).toList();
        assertThat(names).contains(kotlinSkill.getName(), scalaSkill.getName());

        for (SkillEvidence item : evidence) {
            assertThat(item.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(item.getContextSnippet()).isNotBlank();
            assertThat(item.getConfidence()).isGreaterThan(0.0);
            assertThat(item.getUserSkill()).isNull();      // nothing is saved before review
        }

        // Confirm one detected skill at level 4 -> becomes a real profile skill.
        SkillEvidence first = evidence.get(0);
        UserSkill confirmed = evidenceService.confirm(user, first.getId(), 4, 2.0);
        assertThat(confirmed.getLevel()).isEqualTo(4);
        assertThat(userSkillRepository.findByUserIdAndSkillId(user.getId(), first.getSkill().getId())).isPresent();
        assertThat(evidenceService.getOwned(user, first.getId()).getVerificationStatus())
                .isEqualTo(VerificationStatus.CONFIRMED);

        // Reject a false positive; it stays stored as rejected evidence.
        SkillEvidence second = evidence.get(1);
        evidenceService.reject(user, second.getId());
        assertThat(evidenceService.getOwned(user, second.getId()).getVerificationStatus())
                .isEqualTo(VerificationStatus.REJECTED);
        assertThat(userSkillRepository.findByUserIdAndSkillId(user.getId(), second.getSkill().getId()))
                .isEmpty();
    }

    @Test
    void evidenceFromAnotherUsersRunCannotBeTouched() throws Exception {
        byte[] pdf = pdfWithText(kotlinSkill.getName() + " specialist with " + scalaSkill.getName() + " experience.");
        MockMultipartFile file = new MockMultipartFile("file", "their-resume.pdf", "application/pdf", pdf);
        SkillExtractionRun run = documentService.uploadAndExtract(user, file, DocumentType.RESUME);
        SkillEvidence evidence = evidenceService.listForUser(user, run.getId()).get(0);

        String otherEmail = "intruder-" + tag() + "@test.com";
        User other = userRepository.save(new User("Intruder", otherEmail, "hash", Role.USER));

        assertThatThrownBy(() -> evidenceService.confirm(other, evidence.getId(), 5, null))
                .isInstanceOf(com.skillgap.exception.NotFoundException.class);
        assertThatThrownBy(() -> evidenceService.reject(other, evidence.getId()))
                .isInstanceOf(com.skillgap.exception.NotFoundException.class);
    }

    private String tag() {
        return tag;
    }
}
