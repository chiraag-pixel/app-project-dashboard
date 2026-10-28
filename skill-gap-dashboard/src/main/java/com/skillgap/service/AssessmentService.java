package com.skillgap.service;

import com.skillgap.entity.Assessment;
import com.skillgap.entity.AssessmentAnswer;
import com.skillgap.entity.AssessmentAttempt;
import com.skillgap.entity.AssessmentOption;
import com.skillgap.entity.AssessmentQuestion;
import com.skillgap.entity.SkillEvidence;
import com.skillgap.entity.User;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.SourceType;
import com.skillgap.entity.enums.VerificationStatus;
import com.skillgap.exception.BusinessException;
import com.skillgap.exception.NotFoundException;
import com.skillgap.repository.AssessmentAnswerRepository;
import com.skillgap.repository.AssessmentAttemptRepository;
import com.skillgap.repository.AssessmentQuestionRepository;
import com.skillgap.repository.AssessmentRepository;
import com.skillgap.repository.SkillEvidenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creating, taking and grading skill assessments. */
@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final SkillEvidenceRepository evidenceRepository;
    private final UserSkillService userSkillService;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository questionRepository,
                             AssessmentAttemptRepository attemptRepository,
                             AssessmentAnswerRepository answerRepository,
                             SkillEvidenceRepository evidenceRepository,
                             UserSkillService userSkillService) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.evidenceRepository = evidenceRepository;
        this.userSkillService = userSkillService;
    }

    @Transactional(readOnly = true)
    public List<Assessment> listActive() {
        return assessmentRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Assessment> listAll() {
        return assessmentRepository.findAllByOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public Assessment getActive(Long id) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assessment not found"));
        if (!assessment.isActive()) {
            throw new NotFoundException("Assessment not available");
        }
        return assessment;
    }

    @Transactional(readOnly = true)
    public List<AssessmentQuestion> questionsOf(Long assessmentId) {
        return questionRepository.findByAssessmentIdOrderBySortOrderAsc(assessmentId);
    }

    /**
     * Grades a submission: answers is questionId -> optionId.
     * Score = correct / total * 100, rounded.
     */
    @Transactional
    public AssessmentAttempt submit(User user, Long assessmentId, Map<Long, Long> answers) {
        Assessment assessment = getActive(assessmentId);
        List<AssessmentQuestion> questions = questionsOf(assessmentId);
        if (questions.isEmpty()) {
            throw new BusinessException("This assessment has no questions yet.");
        }

        AssessmentAttempt attempt = new AssessmentAttempt(user, assessment, Instant.now());
        // Persist the attempt first: answers carry a non-null FK to it, and
        // NOT NULL score columns need safe defaults before the insert.
        attempt.setCorrectCount(0);
        attempt.setTotalQuestions(questions.size());
        attempt.setScorePercent(0);
        attempt.setPassed(false);
        attempt = attemptRepository.save(attempt);
        int correct = 0;
        for (AssessmentQuestion question : questions) {
            Long optionId = answers == null ? null : answers.get(question.getId());
            AssessmentOption selected = null;
            if (optionId != null) {
                selected = question.getOptions().stream()
                        .filter(o -> o.getId().equals(optionId))
                        .findFirst().orElse(null);
            }
            boolean isCorrect = selected != null && selected.isCorrect();
            if (isCorrect) {
                correct++;
            }
            answerRepository.save(new AssessmentAnswer(attempt, question, selected, isCorrect));
        }

        int score = (int) Math.round(correct / (double) questions.size() * 100.0);
        attempt.setCorrectCount(correct);
        attempt.setTotalQuestions(questions.size());
        attempt.setScorePercent(score);
        attempt.setPassed(score >= assessment.getPassingScore());
        attempt.setCompletedAt(Instant.now());
        return attemptRepository.save(attempt);
    }

    public AssessmentAttempt getAttemptOwned(User user, Long attemptId) {
        return attemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new NotFoundException("Attempt not found"));
    }

    public List<AssessmentAnswer> answersOf(Long attemptId) {
        return answerRepository.findByAttemptId(attemptId);
    }

    @Transactional(readOnly = true)
    public List<AssessmentAttempt> attemptsForUser(User user) {
        return attemptRepository.findByUserIdOrderByCompletedAtDesc(user.getId());
    }

    /** Score -> suggested level. Explicitly applied by the user, never automatic. */
    public int derivedLevel(int scorePercent) {
        if (scorePercent >= 90) {
            return 5;
        }
        if (scorePercent >= 75) {
            return 4;
        }
        if (scorePercent >= 55) {
            return 3;
        }
        if (scorePercent >= 35) {
            return 2;
        }
        return 1;
    }

    /**
     * Applies an attempt's score to the user's skill profile as ASSESSMENT
     * evidence (levels only ever go up automatically; the user can edit down).
     */
    @Transactional
    public UserSkill applyToProfile(User user, Long attemptId) {
        AssessmentAttempt attempt = getAttemptOwned(user, attemptId);
        if (attempt.isAppliedToProfile()) {
            throw new BusinessException("This attempt was already applied to your profile.");
        }
        int level = derivedLevel(attempt.getScorePercent());
        UserSkill userSkill = userSkillService.upsert(user, attempt.getAssessment().getSkill(), level,
                SourceType.ASSESSMENT,
                "Assessment '" + attempt.getAssessment().getTitle() + "' score " + attempt.getScorePercent() + "%");

        SkillEvidence evidence = new SkillEvidence(user, attempt.getAssessment().getSkill(), SourceType.ASSESSMENT);
        evidence.setContextSnippet("Scored " + attempt.getScorePercent() + "% on '"
                + attempt.getAssessment().getTitle() + "' (derived level " + level + ")");
        evidence.setConfidence(Math.min(0.95, attempt.getScorePercent() / 100.0));
        evidence.setVerificationStatus(VerificationStatus.CONFIRMED);
        evidence.setConfirmedLevel(level);
        evidence.setUserSkill(userSkill);
        evidence.setRecordedAt(Instant.now());
        evidenceRepository.save(evidence);

        attempt.setAppliedToProfile(true);
        attemptRepository.save(attempt);
        return userSkill;
    }
}
