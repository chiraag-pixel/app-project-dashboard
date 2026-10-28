package com.skillgap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.skillgap.dto.DashboardData;
import com.skillgap.entity.AnalysisRequirement;
import com.skillgap.entity.Assessment;
import com.skillgap.entity.AssessmentOption;
import com.skillgap.entity.AssessmentQuestion;
import com.skillgap.entity.JobRole;
import com.skillgap.entity.JobRoleSkill;
import com.skillgap.entity.LearningRoadmap;
import com.skillgap.entity.RoadmapTask;
import com.skillgap.entity.Skill;
import com.skillgap.entity.SkillCategory;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.Priority;
import com.skillgap.entity.enums.RequirementType;
import com.skillgap.entity.enums.Role;
import com.skillgap.entity.enums.TaskStatus;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.repository.AnalysisSessionRepository;
import com.skillgap.repository.AssessmentAttemptRepository;
import com.skillgap.repository.AssessmentRepository;
import com.skillgap.repository.GapAnalysisRepository;
import com.skillgap.repository.JobRoleRepository;
import com.skillgap.repository.SkillCategoryRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.repository.UserProfileRepository;
import com.skillgap.repository.UserRepository;
import com.skillgap.repository.UserSkillRepository;
import com.skillgap.service.DashboardService;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.service.RoadmapService;
import com.skillgap.service.WorkSpecService;
import com.skillgap.util.SkillNormalizer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full user journey through the real controllers, services and database:
 * page rendering, skill CRUD, work specification -> confirmed requirements ->
 * analysis -> report -> roadmap -> dashboard, assessment grading, and the
 * rule that users can never read another user's analysis or report.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationFlowTest {

    @Autowired
    private MockMvc mvc;
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
    @Autowired
    private JobRoleRepository jobRoleRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private AssessmentAttemptRepository attemptRepository;
    @Autowired
    private AnalysisSessionRepository sessionRepository;
    @Autowired
    private GapAnalysisRepository gapAnalysisRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private WorkSpecService workSpecService;
    @Autowired
    private GapAnalysisService gapAnalysisService;
    @Autowired
    private RoadmapService roadmapService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private com.skillgap.service.DocumentService documentService;
    @Autowired
    private com.skillgap.service.EvidenceService evidenceService;

    private String tag;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString().substring(0, 8);
    }

    private User newUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User("Flow User " + unique, "flow-" + unique + "@test.com",
                passwordEncoder.encode("Password123!"), Role.USER);
        userRepository.save(user);
        userProfileRepository.save(new UserProfile(user));
        return user;
    }

    private Skill catalogSkill(String name) {
        SkillCategory category = categoryRepository.save(new SkillCategory("Flow Cat " + tag, "test category"));
        return skillRepository.save(new Skill(name, SkillNormalizer.normalize(name), category));
    }

    // ------------------------------------------------------------- page render

    @Test
    void everyMainPageRendersForALoggedInUser() throws Exception {
        User u = newUser();
        List<String> pages = List.of("/dashboard", "/skills", "/documents", "/job-roles",
                "/work-specs", "/analysis", "/analysis/history", "/assessments",
                "/roadmap", "/reports", "/profile", "/notifications");
        for (String page : pages) {
            mvc.perform(get(page).with(user(u.getEmail())))
                    .andExpect(status().isOk());
        }
        // Static assets and the root path work too.
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mvc.perform(get("/vendor/bootstrap.min.css")).andExpect(status().isOk());
        mvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/images/logo.svg")).andExpect(status().isOk());
        // Missing files and unknown routes are real 404s, not 200 error pages.
        mvc.perform(get("/vendor/missing.css")).andExpect(status().isNotFound());
        mvc.perform(get("/no-such-page").with(user(u.getEmail())))
                .andExpect(status().isNotFound());
        // A malformed id in the URL is a client error, not a 200 error page.
        mvc.perform(get("/analysis/not-a-number").with(user(u.getEmail())))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/").with(user(u.getEmail())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    // ---------------------------------------------------------------- skills

    @Test
    void skillCrudWorksAndNeverCreatesDuplicates() throws Exception {
        User u = newUser();
        Skill skill = catalogSkill("FlowSkill" + tag);

        // Add
        mvc.perform(post("/skills/add").with(user(u.getEmail())).with(csrf())
                        .param("skillId", String.valueOf(skill.getId()))
                        .param("level", "3")
                        .param("yearsOfExperience", "1.5")
                        .param("notes", "College project"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/skills"));
        List<UserSkill> rows = userSkillRepository.findWithSkillByUserId(u.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getLevel()).isEqualTo(3);

        // Adding the same skill again updates instead of duplicating
        mvc.perform(post("/skills/add").with(user(u.getEmail())).with(csrf())
                        .param("skillId", String.valueOf(skill.getId()))
                        .param("level", "4")
                        .param("yearsOfExperience", "2")
                        .param("notes", "Internship"))
                .andExpect(status().is3xxRedirection());
        rows = userSkillRepository.findWithSkillByUserId(u.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getLevel()).isEqualTo(4);

        // Edit
        mvc.perform(post("/skills/" + rows.get(0).getId() + "/update").with(user(u.getEmail())).with(csrf())
                        .param("skillId", String.valueOf(skill.getId()))
                        .param("level", "2"))
                .andExpect(status().is3xxRedirection());
        assertThat(userSkillRepository.findWithSkillByUserId(u.getId()).get(0).getLevel()).isEqualTo(2);

        // Delete another user's row is refused
        User other = newUser();
        UserSkill otherSkill = userSkillRepository.save(new UserSkill(other, skill, 5));
        mvc.perform(post("/skills/" + otherSkill.getId() + "/delete").with(user(u.getEmail())).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(userSkillRepository.findById(otherSkill.getId())).isPresent();

        // Delete own row works
        Long ownId = userSkillRepository.findWithSkillByUserId(u.getId()).get(0).getId();
        mvc.perform(post("/skills/" + ownId + "/delete").with(user(u.getEmail())).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(userSkillRepository.findWithSkillByUserId(u.getId())).isEmpty();
    }

    // ------------------------------------------------------------- work spec -> analysis

    @Test
    void workSpecRequirementAnalysisReportAndRoadmapFlow() throws Exception {
        User u = newUser();
        Skill required = catalogSkill("Flowium" + tag);
        UserSkill mine = userSkillRepository.save(new UserSkill(u, required, 2));

        // 1) Create a work specification whose text mentions the required skill.
        mvc.perform(post("/work-specs/add").with(user(u.getEmail())).with(csrf())
                        .param("title", "Build reporting service")
                        .param("description", "The assignment requires advanced knowledge of "
                                + required.getName() + ". Deliverables: REST API and unit tests."))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name(startsWith("redirect:/work-specs/")));

        var documents = workSpecService.listWorkSpecs(u);
        assertThat(documents).hasSize(1);
        Long documentId = documents.get(0).getId();

        // Review page renders and the requirement was detected as PENDING.
        mvc.perform(get("/work-specs/" + documentId).with(user(u.getEmail())))
                .andExpect(status().isOk());
        List<AnalysisRequirement> requirements = workSpecService.listRequirements(u, documentId);
        assertThat(requirements).isNotEmpty();
        AnalysisRequirement requirement = requirements.get(0);
        assertThat(requirement.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(requirement.getContextSnippet()).isNotBlank();

        // 2) Confirm it with an explicit level and weight.
        mvc.perform(post("/work-specs/" + documentId + "/requirements/" + requirement.getId() + "/confirm")
                        .with(user(u.getEmail())).with(csrf())
                        .param("level", "4")
                        .param("weight", "5")
                        .param("type", "REQUIRED"))
                .andExpect(status().is3xxRedirection());
        assertThat(workSpecService.confirmedRequirements(u, documentId)).hasSize(1);

        // 3) Analyse - without confirmation this would have been rejected earlier.
        mvc.perform(post("/work-specs/" + documentId + "/analyze").with(user(u.getEmail())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name(startsWith("redirect:/analysis/")));

        Long sessionId = gapAnalysisService.history(u).get(0).getId();
        var analysis = gapAnalysisService.getAnalysisForSession(sessionId);
        // Current level 2 vs required 4 -> gap 2, gap% 50, alignment 100*(5*min(2,4)/4)/5 = 50.
        assertThat(analysis.getAlignmentPercentage()).isEqualTo(50.0);
        assertThat(gapAnalysisService.getDetails(analysis.getId()).get(0).getGap()).isEqualTo(2);

        // 4) Result page and report render for the owner.
        mvc.perform(get("/analysis/" + sessionId).with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("analysis/result"));
        mvc.perform(get("/reports/" + sessionId).with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/view"));
        mvc.perform(get("/reports").with(user(u.getEmail()))).andExpect(status().isOk());

        // 5) Another user gets a 404 - no cross-user reads.
        User intruder = newUser();
        mvc.perform(get("/reports/" + sessionId).with(user(intruder.getEmail())))
                .andExpect(status().isNotFound());
        mvc.perform(get("/analysis/" + sessionId).with(user(intruder.getEmail())))
                .andExpect(status().isNotFound());

        // 6) Generate the roadmap from this analysis and track progress.
        mvc.perform(post("/roadmap/generate").with(user(u.getEmail())).with(csrf())
                        .param("sessionId", String.valueOf(sessionId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/roadmap"));
        mvc.perform(get("/roadmap").with(user(u.getEmail()))).andExpect(status().isOk());

        LearningRoadmap roadmap = roadmapService.currentRoadmap(u);
        assertThat(roadmap).isNotNull();
        List<RoadmapTask> tasks = roadmapService.tasksOf(roadmap.getId());
        assertThat(tasks).isNotEmpty();
        assertThat(roadmapService.progressPercent(roadmap.getId())).isEqualTo(0.0);

        for (RoadmapTask task : tasks) {
            roadmapService.changeStatus(u, task.getId(), TaskStatus.COMPLETED);
        }
        assertThat(roadmapService.progressPercent(roadmap.getId())).isEqualTo(100.0);

        // 7) Dashboard numbers come from this real data.
        DashboardData data = dashboardService.buildFor(u);
        assertThat(data.skillCount()).isEqualTo(1);
        assertThat(data.hasAnyData()).isTrue();
        assertThat(data.latestSession()).isNotNull();
        assertThat(data.roadmapProgressPercent()).isEqualTo(100.0);
        assertThat(data.roadmapTaskCount()).isEqualTo(tasks.size());
        assertThat(mine.getSkill().getId()).isNotNull();

        mvc.perform(get("/dashboard").with(user(u.getEmail()))).andExpect(status().isOk());
    }

    @Test
    void analysisWithoutConfirmedRequirementsIsRefusedWithClearMessage() throws Exception {
        User u = newUser();
        Skill skill = catalogSkill("Flowium2" + tag);
        var document = workSpecService.createWorkSpec(u, specForm("Empty spec", "Nothing about " + skill.getName() + " here."));

        // No requirement was detected -> nothing confirmed -> analysis must fail.
        assertThat(workSpecService.listRequirements(u, document.getId()).stream()
                .filter(r -> r.getVerificationStatus() == VerificationStatus.CONFIRMED)).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> gapAnalysisService.analyzeDocument(u, document))
                .isInstanceOf(com.skillgap.exception.BusinessException.class)
                .hasMessageContaining("No confirmed requirements");
    }

    // -------------------------------------------------------------- assessments

    @Test
    void assessmentIsGradedByTheServerAndCanBeAppliedToTheProfile() throws Exception {
        User u = newUser();
        Skill skill = catalogSkill("FlowQuiz" + tag);

        Assessment assessment = new Assessment("Flow Quiz " + tag, skill, 60);
        AssessmentQuestion question = new AssessmentQuestion(assessment, "What is 2 + 2?", 1);
        AssessmentOption correct = new AssessmentOption(question, "4", true);
        AssessmentOption wrong = new AssessmentOption(question, "5", false);
        question.addOption(correct);
        question.addOption(wrong);
        assessment.getQuestions().add(question);
        assessmentRepository.save(assessment);

        // Take page renders.
        mvc.perform(get("/assessments/" + assessment.getId() + "/take").with(user(u.getEmail())))
                .andExpect(status().isOk());

        // Submit the correct answer.
        mvc.perform(post("/assessments/" + assessment.getId() + "/submit").with(user(u.getEmail())).with(csrf())
                        .param("question_" + question.getId(), String.valueOf(correct.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name(startsWith("redirect:/assessments/attempts/")));

        Long attemptId = attemptRepository.findByUserIdOrderByCompletedAtDesc(u.getId()).get(0).getId();
        mvc.perform(get("/assessments/attempts/" + attemptId).with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("assessments/result"));

        // 100% score -> derived level 5 when the user explicitly applies it.
        mvc.perform(post("/assessments/attempts/" + attemptId + "/apply").with(user(u.getEmail())).with(csrf()))
                .andExpect(status().is3xxRedirection());
        UserSkill applied = userSkillRepository.findByUserIdAndSkillId(u.getId(), skill.getId()).orElseThrow();
        assertThat(applied.getLevel()).isEqualTo(5);
        assertThat(applied.getSourceType()).isEqualTo(com.skillgap.entity.enums.SourceType.ASSESSMENT);

        // Applying twice is refused with a friendly message, not a stack trace.
        mvc.perform(post("/assessments/attempts/" + attemptId + "/apply").with(user(u.getEmail())).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Another user cannot open this attempt.
        User intruder = newUser();
        mvc.perform(get("/assessments/attempts/" + attemptId).with(user(intruder.getEmail())))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------- job roles

    @Test
    void predefinedRoleAnalysisAndRoleDetailPagesWork() throws Exception {
        User u = newUser();
        Skill skill = catalogSkill("FlowRole" + tag);

        JobRole role = new JobRole("Flow Role " + tag, "role for the flow test");
        role.addSkill(new JobRoleSkill(skill, 3, 4, RequirementType.REQUIRED));
        role = jobRoleRepository.save(role);
        userSkillRepository.save(new UserSkill(u, skill, 3));

        mvc.perform(get("/job-roles").with(user(u.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/job-roles/" + role.getId()).with(user(u.getEmail()))).andExpect(status().isOk());

        mvc.perform(post("/analysis/run/role").with(user(u.getEmail())).with(csrf())
                        .param("jobRoleId", String.valueOf(role.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name(startsWith("redirect:/analysis/")));

        Long sessionId = gapAnalysisService.history(u).get(0).getId();
        var analysis = gapAnalysisService.getAnalysisForSession(sessionId);
        assertThat(analysis.getAlignmentPercentage()).isEqualTo(100.0);  // meets required level
        assertThat(analysis.getMeetsCount()).isEqualTo(1);
        assertThat(gapAnalysisRepository.findBySessionId(sessionId)).isPresent();

        // Dashboard shows the alignment from the saved analysis.
        DashboardData data = dashboardService.buildFor(u);
        assertThat(data.latestAnalysis().getAlignmentPercentage()).isEqualTo(100.0);
        assertThat(data.topGaps()).isEmpty();     // everything meets the target
    }

    // ------------------------------------------------------- resume review page

    @Test
    void resumeUploadAndEvidenceReviewPageRender() throws Exception {
        User u = newUser();
        Skill skill = catalogSkill("FlowCv" + tag);

        byte[] pdf;
        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            try (org.apache.pdfbox.pdmodel.PDPageContentStream content =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(document, document.getPage(0))) {
                content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                content.beginText();
                content.newLineAtOffset(50, 700);
                content.showText("Worked with " + skill.getName() + " on several projects.");
                content.endText();
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            document.save(out);
            pdf = out.toByteArray();
        }

        mvc.perform(multipart("/documents/resume/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "cv.pdf", "application/pdf", pdf))
                        .with(user(u.getEmail())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name(startsWith("redirect:/documents/runs/")));

        var runs = documentService.listRunsForUser(u);
        assertThat(runs).hasSize(1);
        assertThat(evidenceService.listForUser(u, runs.get(0).getId())).isNotEmpty();

        mvc.perform(get("/documents").with(user(u.getEmail()))).andExpect(status().isOk());
        mvc.perform(get("/documents/runs/" + runs.get(0).getId()).with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("documents/review"));
    }

    // ------------------------------------------------------------------ helpers

    private com.skillgap.dto.WorkSpecForm specForm(String title, String description) {
        com.skillgap.dto.WorkSpecForm form = new com.skillgap.dto.WorkSpecForm();
        form.setTitle(title);
        form.setDescription(description);
        return form;
    }
}
