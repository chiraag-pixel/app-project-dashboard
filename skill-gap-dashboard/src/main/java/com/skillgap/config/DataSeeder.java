package com.skillgap.config;

import com.skillgap.entity.Assessment;
import com.skillgap.entity.AssessmentOption;
import com.skillgap.entity.AssessmentQuestion;
import com.skillgap.entity.JobRole;
import com.skillgap.entity.JobRoleSkill;
import com.skillgap.entity.LearningResource;
import com.skillgap.entity.Skill;
import com.skillgap.entity.SkillCategory;
import com.skillgap.entity.User;
import com.skillgap.entity.UserProfile;
import com.skillgap.entity.UserSkill;
import com.skillgap.entity.enums.RequirementType;
import com.skillgap.entity.enums.Role;
import com.skillgap.entity.enums.SourceType;
import com.skillgap.repository.AssessmentRepository;
import com.skillgap.repository.JobRoleRepository;
import com.skillgap.repository.LearningResourceRepository;
import com.skillgap.repository.SkillCategoryRepository;
import com.skillgap.repository.SkillRepository;
import com.skillgap.repository.UserProfileRepository;
import com.skillgap.repository.UserRepository;
import com.skillgap.repository.UserSkillRepository;
import com.skillgap.util.SkillNormalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds catalog data on first start: skill categories, common skills, seven job
 * roles with required levels/weights, sample assessments with questions, and
 * learning resources. Optionally creates clearly labelled demo accounts
 * (demo@example.com / admin@example.com - see README for the demo passwords).
 *
 * Seeding is idempotent: when categories already exist it does nothing.
 * User-specific scores, analyses and progress are NEVER seeded - they only come
 * from real application usage.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final JobRoleRepository jobRoleRepository;
    private final AssessmentRepository assessmentRepository;
    private final LearningResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSkillRepository userSkillRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public DataSeeder(SkillCategoryRepository categoryRepository,
                      SkillRepository skillRepository,
                      JobRoleRepository jobRoleRepository,
                      AssessmentRepository assessmentRepository,
                      LearningResourceRepository resourceRepository,
                      UserRepository userRepository,
                      UserProfileRepository userProfileRepository,
                      UserSkillRepository userSkillRepository,
                      PasswordEncoder passwordEncoder,
                      AppProperties properties) {
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.assessmentRepository = assessmentRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSkillRepository = userSkillRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            log.info("Seed data already present - skipping.");
            return;
        }
        log.info("Seeding catalog data (categories, skills, roles, assessments, resources)...");

        Map<String, SkillCategory> categories = seedCategories();
        Map<String, Skill> skills = seedSkills(categories);
        seedJobRoles(skills);
        seedAssessments(skills);
        seedResources(skills);
        if (properties.isSeedDemoData()) {
            seedDemoUsers(skills);
        }
        log.info("Seed data ready.");
    }

    // ---------------------------------------------------------------- categories

    private Map<String, SkillCategory> seedCategories() {
        Map<String, SkillCategory> map = new LinkedHashMap<>();
        String[][] data = {
                {"Programming Languages", "Core programming languages"},
                {"Web Development", "Frontend and backend web technologies"},
                {"Databases", "Relational and non-relational databases"},
                {"Data Structures and Algorithms", "Computer science fundamentals"},
                {"AI and Machine Learning", "Data science and ML technologies"},
                {"Cloud and DevOps", "Cloud platforms, containers and delivery"},
                {"Automation", "Testing and build automation"},
                {"Communication", "Soft skills"},
                {"Project Management", "Planning and delivery skills"},
                {"Other", "Skills added by users"}
        };
        for (String[] row : data) {
            map.put(row[0], categoryRepository.save(new SkillCategory(row[0], row[1])));
        }
        return map;
    }

    // ------------------------------------------------------------------- skills

    private Map<String, Skill> seedSkills(Map<String, SkillCategory> categories) {
        Map<String, Skill> map = new LinkedHashMap<>();
        // name, category, aliases used by the rule-based extraction engine
        String[][] data = {
                {"Java", "Programming Languages", "jdk, j2ee"},
                {"Python", "Programming Languages", "python3"},
                {"JavaScript", "Programming Languages", "js, ecmascript"},
                {"C#", "Programming Languages", "c sharp, csharp"},
                {"TypeScript", "Programming Languages", "ts"},
                {"SQL", "Databases", "structured query language, pl/sql, t-sql"},
                {"HTML", "Web Development", "html5"},
                {"CSS", "Web Development", "css3, stylesheets, bootstrap"},
                {"Spring Boot", "Web Development", "springboot, spring mvc, spring framework"},
                {"REST APIs", "Web Development", "rest, restful, api design"},
                {"React", "Web Development", "reactjs, react.js"},
                {"Angular", "Web Development", "angularjs"},
                {"Hibernate", "Web Development", "jpa, orm"},
                {"Thymeleaf", "Web Development", "server side templates"},
                {"MySQL", "Databases", "mariadb"},
                {"PostgreSQL", "Databases", "postgres"},
                {"MongoDB", "Databases", "nosql, document database"},
                {"Redis", "Databases", "caching, cache"},
                {"Data Structures", "Data Structures and Algorithms", "arrays, linked lists, trees, hashmaps, stacks, queues"},
                {"Algorithms", "Data Structures and Algorithms", "sorting, dynamic programming"},
                {"System Design", "Data Structures and Algorithms", "distributed systems, scalability"},
                {"Problem Solving", "Communication", "critical thinking, analytical skills"},
                {"Machine Learning", "AI and Machine Learning", "ml, scikit-learn, sklearn"},
                {"Deep Learning", "AI and Machine Learning", "neural networks, pytorch"},
                {"TensorFlow", "AI and Machine Learning", "keras"},
                {"Data Analysis", "AI and Machine Learning", "data analytics, pandas, numpy, matplotlib, power bi, tableau"},
                {"Statistics", "AI and Machine Learning", "statistical analysis, probability"},
                {"Git", "Cloud and DevOps", "github, gitlab, version control"},
                {"Docker", "Cloud and DevOps", "containers, containerization"},
                {"Kubernetes", "Cloud and DevOps", "k8s"},
                {"AWS", "Cloud and DevOps", "amazon web services, ec2, s3"},
                {"CI/CD", "Cloud and DevOps", "continuous integration, continuous delivery, continuous deployment, github actions, jenkins"},
                {"Linux", "Cloud and DevOps", "bash, shell scripting, ubuntu, unix"},
                {"Maven", "Automation", "gradle"},
                {"JUnit", "Automation", "mockito, unit testing"},
                {"Selenium", "Automation", "test automation, ui automation"},
                {"Communication", "Communication", "verbal communication, written communication, presentation, teamwork"},
                {"Project Management", "Project Management", "agile, scrum, kanban, jira"}
        };
        for (String[] row : data) {
            Skill skill = new Skill(row[0], SkillNormalizer.normalize(row[0]), categories.get(row[1]));
            skill.setAliases(row[2]);
            map.put(row[0], skillRepository.save(skill));
        }
        return map;
    }

    // ----------------------------------------------------------------- job roles

    private void seedJobRoles(Map<String, Skill> s) {
        createRole("Java Developer", "Develops backend services with the Java ecosystem.",
                row(s, "Java", 4, 5, true), row(s, "Spring Boot", 4, 5, true), row(s, "SQL", 3, 4, true),
                row(s, "MySQL", 3, 3, true), row(s, "REST APIs", 3, 4, true), row(s, "Git", 3, 3, true),
                row(s, "Data Structures", 3, 3, true), row(s, "Hibernate", 3, 2, false),
                row(s, "JUnit", 3, 2, false), row(s, "Maven", 2, 2, false),
                row(s, "Docker", 2, 2, false), row(s, "Communication", 3, 2, true));
        createRole("Backend Developer", "Builds server-side APIs and services.",
                row(s, "Java", 3, 4, true), row(s, "Spring Boot", 3, 4, true), row(s, "REST APIs", 4, 5, true),
                row(s, "SQL", 4, 5, true), row(s, "MySQL", 3, 3, true), row(s, "Git", 3, 3, true),
                row(s, "Data Structures", 3, 3, true), row(s, "System Design", 3, 2, false),
                row(s, "Docker", 2, 2, false), row(s, "Python", 3, 2, false),
                row(s, "Communication", 3, 2, true));
        createRole("Frontend Developer", "Builds user interfaces in the browser.",
                row(s, "JavaScript", 4, 5, true), row(s, "HTML", 4, 4, true), row(s, "CSS", 4, 4, true),
                row(s, "React", 3, 4, true), row(s, "TypeScript", 3, 2, false),
                row(s, "REST APIs", 3, 2, true), row(s, "Git", 3, 2, true),
                row(s, "Communication", 3, 2, true));
        createRole("Full-Stack Developer", "Works across frontend and backend.",
                row(s, "Java", 3, 4, true), row(s, "JavaScript", 3, 4, true), row(s, "Spring Boot", 3, 4, true),
                row(s, "React", 3, 3, true), row(s, "HTML", 3, 3, true), row(s, "CSS", 3, 3, true),
                row(s, "SQL", 3, 4, true), row(s, "MySQL", 3, 3, true), row(s, "REST APIs", 3, 4, true),
                row(s, "Git", 3, 3, true), row(s, "Communication", 3, 2, true));
        createRole("Data Analyst", "Analyses data and produces insights.",
                row(s, "SQL", 4, 5, true), row(s, "Python", 4, 5, true), row(s, "Data Analysis", 4, 5, true),
                row(s, "Statistics", 3, 3, true), row(s, "Communication", 4, 3, true),
                row(s, "Data Structures", 2, 1, false), row(s, "Machine Learning", 2, 2, false));
        createRole("AI/ML Engineer", "Designs and trains machine learning models.",
                row(s, "Python", 4, 5, true), row(s, "Machine Learning", 4, 5, true),
                row(s, "Deep Learning", 3, 4, true), row(s, "Statistics", 3, 3, true),
                row(s, "SQL", 3, 3, true), row(s, "TensorFlow", 3, 2, false),
                row(s, "Data Structures", 3, 2, false), row(s, "Git", 2, 2, false),
                row(s, "Communication", 3, 2, true));
        createRole("DevOps Engineer", "Automates builds, delivery and infrastructure.",
                row(s, "Linux", 4, 4, true), row(s, "Docker", 4, 5, true), row(s, "Kubernetes", 3, 4, true),
                row(s, "AWS", 3, 4, true), row(s, "CI/CD", 4, 4, true), row(s, "Git", 3, 3, true),
                row(s, "Maven", 2, 2, false), row(s, "Python", 2, 2, false),
                row(s, "Communication", 3, 2, true));
    }

    private JobRoleSkill row(Map<String, Skill> skills, String skillName, int level, int weight, boolean required) {
        return new JobRoleSkill(skills.get(skillName), level, weight,
                required ? RequirementType.REQUIRED : RequirementType.PREFERRED);
    }

    private void createRole(String name, String description, JobRoleSkill... rows) {
        JobRole role = new JobRole(name, description);
        for (JobRoleSkill row : rows) {
            role.addSkill(row);
        }
        jobRoleRepository.save(role);
    }

    // -------------------------------------------------------------- assessments

    private void seedAssessments(Map<String, Skill> s) {
        createAssessment("Java Basics", "Core Java knowledge check.", s.get("Java"), 60,
                q("Which keyword is used to inherit a class in Java?",
                        o("implements", false), o("extends", true), o("inherits", false), o("superclass", false)),
                q("What is the default value of an int field in Java?",
                        o("0", true), o("null", false), o("-1", false), o("undefined", false)),
                q("Which of these is NOT a primitive type in Java?",
                        o("int", false), o("double", false), o("String", true), o("boolean", false)),
                q("What does JVM stand for?",
                        o("Java Virtual Machine", true), o("Java Variable Method", false),
                        o("Joint Virtual Memory", false), o("Java Verified Mode", false)),
                q("Which collection type does not allow duplicate elements?",
                        o("ArrayList", false), o("HashSet", true), o("LinkedList", false), o("ArrayDeque", false)));

        createAssessment("SQL Fundamentals", "Basic SQL and relational database knowledge.", s.get("SQL"), 60,
                q("Which clause filters rows before grouping?",
                        o("ORDER BY", false), o("WHERE", true), o("LIMIT", false), o("OFFSET", false)),
                q("Which function counts the number of rows?",
                        o("SUM()", false), o("COUNT()", true), o("TOTAL()", false), o("NUM()", false)),
                q("What does an INNER JOIN return?",
                        o("Only rows that match in both tables", true), o("All rows from the left table", false),
                        o("All rows from both tables", false), o("No rows at all", false)),
                q("Which command removes all rows from a table while keeping its structure?",
                        o("DROP TABLE", false), o("TRUNCATE TABLE", true), o("DELETE DATABASE", false),
                        o("REMOVE TABLE", false)),
                q("A PRIMARY KEY column must be...",
                        o("unique and not null", true), o("allowed to contain null", false),
                        o("always of type text", false), o("the same across tables", false)));

        createAssessment("JavaScript Basics", "Fundamental JavaScript knowledge.", s.get("JavaScript"), 60,
                q("Which keyword declares a block-scoped variable?",
                        o("var", false), o("let", true), o("define", false), o("local", false)),
                q("What does the === operator compare?",
                        o("Value and type", true), o("Value only", false),
                        o("Variable names only", false), o("Nothing", false)),
                q("Which method converts JSON text into a JavaScript object?",
                        o("JSON.parse()", true), o("JSON.stringify()", false),
                        o("JSON.convert()", false), o("Object.from()", false)),
                q("Which symbol starts a single-line comment?",
                        o("//", true), o("#", false), o("--", false), o("::", false)));

        createAssessment("Git Fundamentals", "Everyday Git usage.", s.get("Git"), 50,
                q("Which command creates a new branch?",
                        o("git branch feature", true), o("git init", false),
                        o("git remote add", false), o("git tag", false)),
                q("Which command stages changes for a commit?",
                        o("git add .", true), o("git push", false), o("git log", false), o("git reset --hard", false)),
                q("What does git clone do?",
                        o("Copies a remote repository to your computer", true),
                        o("Uploads your local code", false), o("Merges two branches", false),
                        o("Deletes a remote repository", false)),
                q("Which command shows the commit history?",
                        o("git log", true), o("git status", false), o("git diff", false), o("git fetch", false)));
    }

    private record Option(String text, boolean correct) {
    }

    private Option o(String text, boolean correct) {
        return new Option(text, correct);
    }

    private AssessmentQuestion q(String text, Option... options) {
        AssessmentQuestion question = new AssessmentQuestion(null, text, 1);
        for (Option option : options) {
            question.addOption(new AssessmentOption(question, option.text(), option.correct()));
        }
        return question;
    }

    private void createAssessment(String title, String description, Skill skill, int passingScore,
                                  AssessmentQuestion... questions) {
        Assessment assessment = new Assessment(title, skill, passingScore);
        assessment.setDescription(description);
        int order = 1;
        for (AssessmentQuestion question : questions) {
            question.setAssessment(assessment);
            question.setSortOrder(order++);
            assessment.getQuestions().add(question);
        }
        assessmentRepository.save(assessment);
    }

    // ---------------------------------------------------------------- resources

    private void seedResources(Map<String, Skill> s) {
        resource("Java Tutorial (Oracle)", "https://docs.oracle.com/javase/tutorial/", s.get("Java"), 1, 10.0,
                "Official Java tutorial covering language fundamentals.");
        resource("Spring Boot Guides", "https://spring.io/guides", s.get("Spring Boot"), 3, 12.0,
                "Official Spring guides, including REST service tutorials.");
        resource("MDN JavaScript Guide", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide",
                s.get("JavaScript"), 2, 10.0, "Comprehensive JavaScript reference on MDN.");
        resource("MDN HTML Basics", "https://developer.mozilla.org/en-US/docs/Learn/Getting_started_with_the_web/HTML_basics",
                s.get("HTML"), 1, 3.0, "Start here if you are new to HTML.");
        resource("MDN CSS Basics", "https://developer.mozilla.org/en-US/docs/Learn/Getting_started_with_the_web/CSS_basics",
                s.get("CSS"), 1, 4.0, "Styling web pages with CSS.");
        resource("W3Schools MySQL Tutorial", "https://www.w3schools.com/mysql/", s.get("MySQL"), 2, 8.0,
                "Step-by-step MySQL examples.");
        resource("SQLBolt Interactive Lessons", "https://sqlbolt.com/", s.get("SQL"), 1, 5.0,
                "Learn SQL with interactive exercises.");
        resource("Learn Git Branching", "https://learngitbranching.js.org/", s.get("Git"), 2, 4.0,
                "Visual, interactive Git practice.");
        resource("Docker Get Started", "https://docs.docker.com/get-started/", s.get("Docker"), 2, 6.0,
                "Official Docker beginner tutorial.");
        resource("Kubernetes Basics", "https://kubernetes.io/docs/tutorials/kubernetes-basics/",
                s.get("Kubernetes"), 3, 6.0, "Hands-on Kubernetes introduction.");
        resource("Python Official Tutorial", "https://docs.python.org/3/tutorial/", s.get("Python"), 1, 10.0,
                "The official Python tutorial.");
        resource("Kaggle Data Analysis", "https://www.kaggle.com/learn/data-analysis",
                s.get("Data Analysis"), 2, 8.0, "Free short course on data analysis.");
        resource("Machine Learning Course", "https://www.coursera.org/learn/machine-learning",
                s.get("Machine Learning"), 3, 20.0, "Classic introductory ML course.");
        resource("AWS Cloud Practitioner Essentials", "https://explore.skillbuilder.aws/learn/public/learning_plan/view/20/cloud-technical-essentials",
                s.get("AWS"), 2, 10.0, "Free AWS foundational training.");
        resource("Atlassian Agile Guide", "https://www.atlassian.com/agile/kanban", s.get("Project Management"), 2, 4.0,
                "Practical introduction to agile and kanban.");
        resource("Coursera Communication Skills", "https://www.coursera.org/learn/whats-special-communication",
                s.get("Communication"), 2, 6.0, "Short course on workplace communication.");
        resource("Coursera Data Structures", "https://www.coursera.org/learn/data-structures",
                s.get("Data Structures"), 2, 12.0, "University course on core data structures.");
        resource("RESTful API Design Guide", "https://restfulapi.net/", s.get("REST APIs"), 2, 3.0,
                "Guidelines for designing good REST APIs.");
        resource("Baeldung Spring Boot", "https://www.baeldung.com/spring-boot", s.get("Spring Boot"), 3, 15.0,
                "Practical Spring Boot tutorials.");
        resource("Linux Command Line Tutorial", "https://linuxcommand.org/", s.get("Linux"), 2, 8.0,
                "Learn the Linux shell step by step.");
    }

    private void resource(String title, String url, Skill skill, int difficulty, double hours, String description) {
        LearningResource resource = new LearningResource(title, url, skill);
        resource.setDifficulty(difficulty);
        resource.setEstimatedHours(hours);
        resource.setDescription(description);
        resourceRepository.save(resource);
    }

    // --------------------------------------------------------------- demo users

    /**
     * Clearly labelled demo accounts so the project can be demonstrated quickly:
     *   demo@example.com  / Demo@123   (regular user with a few starter skills)
     *   admin@example.com / Admin@123  (administrator)
     * They contain NO fabricated analysis results or scores.
     */
    private void seedDemoUsers(Map<String, Skill> s) {
        if (userRepository.existsByEmail("demo@example.com")) {
            return;
        }
        User demo = new User("Demo User", "demo@example.com", passwordEncoder.encode("Demo@123"), Role.USER);
        userRepository.save(demo);

        UserProfile profile = new UserProfile(demo);
        profile.setEducation("B.Tech Computer Science, 2025");
        profile.setExperience("Two internships; college projects in Java and web development");
        profile.setCareerInterests("Backend development, cloud technologies");
        profile.setTargetJobRole(jobRoleRepository.findByNameIgnoreCase("Java Developer").orElse(null));
        userProfileRepository.save(profile);

        userSkill(demo, s.get("Java"), 3, "Built a library management system in a college project");
        userSkill(demo, s.get("SQL"), 2, "Used MySQL in course work");
        userSkill(demo, s.get("HTML"), 3, null);
        userSkill(demo, s.get("CSS"), 2, null);

        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = new User("Admin User", "admin@example.com", passwordEncoder.encode("Admin@123"), Role.ADMIN);
            userRepository.save(admin);
            userProfileRepository.save(new UserProfile(admin));
        }
    }

    private void userSkill(User user, Skill skill, int level, String notes) {
        UserSkill userSkill = new UserSkill(user, skill, level);
        userSkill.setNotes(notes);
        userSkill.setSourceType(SourceType.MANUAL);
        userSkillRepository.save(userSkill);
    }
}
