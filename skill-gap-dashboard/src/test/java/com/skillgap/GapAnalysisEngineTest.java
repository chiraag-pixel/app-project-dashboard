package com.skillgap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skillgap.entity.GapAnalysis;
import com.skillgap.entity.GapAnalysisDetail;
import com.skillgap.entity.AnalysisSession;
import com.skillgap.entity.JobRole;
import com.skillgap.entity.JobRoleSkill;
import com.skillgap.entity.Skill;
import com.skillgap.entity.SkillCategory;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.Priority;
import com.skillgap.entity.enums.RequirementType;
import com.skillgap.entity.enums.Role;
import com.skillgap.exception.BusinessException;
import com.skillgap.repository.GapAnalysisRepository;
import com.skillgap.repository.JobRoleRepository;
import com.skillgap.repository.SkillCategoryRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.repository.UserProfileRepository;
import com.skillgap.repository.UserRepository;
import com.skillgap.repository.UserSkillRepository;
import com.skillgap.service.GapAnalysisService;
import com.skillgap.util.SkillNormalizer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the formulas from the specification:
 *   Gap            = max(0, Required Level - Current Level)
 *   Gap Percentage = Gap / Required Level x 100
 *   Alignment      = 100 x sum(weight x min(current, required) / required) / sum(weights)
 * plus deterministic priority rules and unassessed-skill handling.
 */
@SpringBootTest
class GapAnalysisEngineTest {

    @Autowired
    private GapAnalysisService gapAnalysisService;
    @Autowired
    private JobRoleRepository jobRoleRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private SkillCategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserSkillRepository userSkillRepository;
    @Autowired
    private GapAnalysisRepository gapAnalysisRepository;

    private User user;
    private String tag;
    private SkillCategory category;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(
                new User("Engine Tester " + tag, "engine-" + tag + "@test.com", "unused-hash", Role.USER));
        userProfileRepository.save(new UserProfile(user));
        category = categoryRepository.save(new SkillCategory("Engine Cat " + tag, "test category"));
    }

    private Skill skill(String name) {
        Skill skill = new Skill(name, SkillNormalizer.normalize(name), category);
        return skillRepository.save(skill);
    }

    private GapAnalysisDetail detailFor(Skill skill, List<GapAnalysisDetail> details) {
        return details.stream()
                .filter(d -> d.getSkill().getId().equals(skill.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void calculatesGapsAlignmentAndPrioritiesFromDatabaseData() {
        Skill a = skill("Engine Alpha " + tag);
        Skill b = skill("Engine Beta " + tag);
        Skill c = skill("Engine Gamma " + tag);
        Skill d = skill("Engine Delta " + tag);

        // Required weights: A=5, B=4, D=3 (preferred C=1 excluded from alignment)
        JobRole role = new JobRole("Test Role " + tag, "role used by engine tests");
        role.addSkill(new JobRoleSkill(a, 4, 5, RequirementType.REQUIRED));
        role.addSkill(new JobRoleSkill(b, 4, 4, RequirementType.REQUIRED));
        role.addSkill(new JobRoleSkill(c, 2, 1, RequirementType.PREFERRED));
        role.addSkill(new JobRoleSkill(d, 3, 3, RequirementType.REQUIRED));
        role = jobRoleRepository.save(role);

        // The user has A at level 2 and D at level 5; B and C are unassessed.
        userSkillRepository.save(new UserSkill(user, a, 2));
        userSkillRepository.save(new UserSkill(user, d, 5));

        AnalysisSession session = gapAnalysisService.analyzeJobRole(user, role.getId());

        // Alignment = 100 * (5*min(2,4)/4 + 4*0/4 + 3*min(5,3)/3) / (5+4+3)
        //           = 100 * (2.5 + 0 + 3) / 12 = 45.8
        assertThat(session.getAlignmentPercentage()).isEqualTo(45.8);
        assertThat(session.getRequiredSkillCount()).isEqualTo(3);
        assertThat(session.getCriticalGapCount()).isEqualTo(1);

        GapAnalysis analysis = gapAnalysisRepository.findBySessionId(session.getId()).orElseThrow();
        assertThat(analysis.getMeetsCount()).isEqualTo(1);          // D
        assertThat(analysis.getUnassessedCount()).isEqualTo(2);     // B and C
        assertThat(analysis.getPartialCount()).isEqualTo(1);        // A (B and C reported separately)
        assertThat(analysis.getCriticalCount()).isEqualTo(1);       // B
        assertThat(analysis.getTotalWeight()).isEqualTo(12.0);
        assertThat(analysis.getNotes()).contains("2 skill(s) have not been assessed");

        List<GapAnalysisDetail> details = gapAnalysisService.getDetails(analysis.getId());
        assertThat(details).hasSize(4);

        GapAnalysisDetail da = detailFor(a, details);
        assertThat(da.getCurrentLevel()).isEqualTo(2);
        assertThat(da.getRequiredLevel()).isEqualTo(4);
        assertThat(da.getGap()).isEqualTo(2);                       // max(0, 4 - 2)
        assertThat(da.getGapPercentage()).isEqualTo(50.0);          // 2 / 4 * 100
        assertThat(da.getPriority()).isEqualTo(Priority.HIGH);      // 5 * 2 = 6
        assertThat(da.isAssessed()).isTrue();

        GapAnalysisDetail db = detailFor(b, details);
        assertThat(db.getGap()).isEqualTo(4);
        assertThat(db.getGapPercentage()).isEqualTo(100.0);
        assertThat(db.getPriority()).isEqualTo(Priority.CRITICAL);  // gap >= 3 and weight >= 4
        assertThat(db.isAssessed()).isFalse();
        assertThat(db.getCurrentLevel()).isNull();                  // unassessed is not a confirmed 0

        GapAnalysisDetail dc = detailFor(c, details);
        assertThat(dc.getRequirementType()).isEqualTo(RequirementType.PREFERRED);
        assertThat(dc.getPriority()).isEqualTo(Priority.LOW);       // 1 * 2 = 2

        GapAnalysisDetail dd = detailFor(d, details);
        assertThat(dd.getGap()).isEqualTo(0);
        assertThat(dd.getGapPercentage()).isEqualTo(0.0);
        assertThat(dd.getPriority()).isEqualTo(Priority.MET);
        assertThat(dd.getRecommendation()).isNotBlank();
    }

    @Test
    void priorityRulesAreDeterministic() {
        assertThat(gapAnalysisService.priorityFor(0, 5)).isEqualTo(Priority.MET);
        assertThat(gapAnalysisService.priorityFor(1, 1)).isEqualTo(Priority.LOW);      // score 1
        assertThat(gapAnalysisService.priorityFor(2, 2)).isEqualTo(Priority.MEDIUM);   // score 4
        assertThat(gapAnalysisService.priorityFor(1, 5)).isEqualTo(Priority.MEDIUM);   // score 5
        assertThat(gapAnalysisService.priorityFor(2, 3)).isEqualTo(Priority.HIGH);     // score 6
        assertThat(gapAnalysisService.priorityFor(3, 4)).isEqualTo(Priority.CRITICAL); // gap>=3, weight>=4
        assertThat(gapAnalysisService.priorityFor(2, 5)).isEqualTo(Priority.HIGH);     // score 10
        assertThat(gapAnalysisService.priorityFor(3, 4)).isEqualTo(gapAnalysisService.priorityFor(3, 4));
    }

    @Test
    void unassessedSkillsLowerAlignmentButAreExplained() {
        Skill requiredSkill = skill("Engine Unknown " + tag);
        JobRole role = new JobRole("Single Skill Role " + tag, "one required skill");
        role.addSkill(new JobRoleSkill(requiredSkill, 5, 5, RequirementType.REQUIRED));
        role = jobRoleRepository.save(role);

        AnalysisSession session = gapAnalysisService.analyzeJobRole(user, role.getId());

        // Nothing assessed: alignment is 0, and the note explains unassessed != no ability.
        assertThat(session.getAlignmentPercentage()).isEqualTo(0.0);
        GapAnalysis analysis = gapAnalysisRepository.findBySessionId(session.getId()).orElseThrow();
        assertThat(analysis.getUnassessedCount()).isEqualTo(1);
        assertThat(analysis.getNotes()).contains("unassessed does NOT mean you have no ability");

        // After recording the skill at the required level, alignment becomes 100.
        userSkillRepository.save(new UserSkill(user, requiredSkill, 5));
        AnalysisSession second = gapAnalysisService.analyzeJobRole(user, role.getId());
        assertThat(second.getAlignmentPercentage()).isEqualTo(100.0);
    }

    @Test
    void roleWithoutRequirementsIsRejectedWithFriendlyError() {
        JobRole empty = jobRoleRepository.save(new JobRole("Empty Role " + tag, "no skills configured"));
        assertThatThrownBy(() -> gapAnalysisService.analyzeJobRole(user, empty.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no configured skills");
    }
}
