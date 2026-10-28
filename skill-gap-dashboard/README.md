# Skill Gap Analysis Dashboard

A full-stack college project that compares the skills a person **currently has** with the skills
required for a **job role, job description or work assignment**, calculates the gaps, and
recommends what to learn next.

Everything runs on a real backend (Spring Boot + MySQL) with server-rendered Thymeleaf pages.
There are no hardcoded dashboard numbers - every value on screen is computed from database data.

---

## 1. Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot (Spring MVC, Spring Data JPA/Hibernate, Spring Security, Bean Validation) |
| Frontend | HTML5, CSS3, JavaScript, Thymeleaf, Bootstrap 5, Chart.js |
| Database | MySQL 8 (MySQL Connector/J), schema generated from JPA entities (`ddl-auto=update`) |
| Documents | Apache PDFBox (PDF), Apache POI (DOCX) |
| Tests | JUnit 5, Spring Boot Test, MockMvc, H2 (in-memory, test scope only) |
| Build | Maven |

---

## 2. Implemented features

**Authentication & users**
- Registration with server-side validation, BCrypt password hashing, immediate sign-in
- Form login / logout, session-based security, CSRF protection on every form
- Login throttle (5 failed attempts -> 5 minute lockout, in-memory)
- Roles: `USER` and `ADMIN`, enforced server-side on `/admin/**` (not just hidden buttons)
- Profile: name, email, education, experience, career interests, target job role
- Users can only ever read their own skills, documents, analyses, attempts and reports

**Skill input methods**
- Manual skill entry (catalog picker or new skill name) with proficiency 1-5, years, notes;
  edit and delete; duplicates update instead of creating a second row
- Resume upload (PDF/DOCX) -> private file storage -> PDFBox/POI text extraction ->
  rule-based catalog matching -> PENDING evidence for review -> confirm/reject per skill
- Job description paste or upload -> requirement extraction -> review (level/weight/type) -> confirm
- Work specification (scope, deliverables, tools) -> requirement extraction -> review -> analysis
- Projects & certifications recorded on the Profile page (evidence, never automatic proof)
- Assessments: quizzes are graded by the server; applying a score to the profile only happens
  when the user presses "Apply score to my profile"
- Graceful failures: wrong extension, oversized, corrupt, encrypted and scanned/image-only files
  all produce friendly messages. **OCR is not supported and never claimed.**

**Skill catalog & roles**
- 10 categories, ~39 common skills (with aliases for matching), 7 predefined job roles with
  required levels, importance weights and REQUIRED/PREFERRED types
- Admin can manage categories, skills, roles and role requirements

**Gap analysis engine (deterministic, see §5)**
- Per skill: current level, required level, gap, gap %, weight, priority, recommended resource
- Weighted alignment over REQUIRED skills; unassessed skills handled separately and explained
- Every analysis is saved with timestamp, target and per-skill rows

**Dashboard** - real metrics from the database: skill count, completed assessments, target role,
alignment %, priority gaps, roadmap progress, current-vs-required bar chart (Chart.js), radar
chart of top competencies, recent attempts and analyses, plus empty states for new users.

**Learning roadmap** - generate tasks from an analysis (ordered by priority, linked to seeded
resources), add/edit/complete tasks, live completion percentage feeding the dashboard.

**Reports** - printable report per analysis (browser Print / Save as PDF via print stylesheet),
owner-checked; admins cannot open other users' reports either.

**Admin panel** - users (role/enable), categories, skills, roles + requirements, assessments +
questions, learning resources, statistics and an audit log. All endpoints role-checked by
Spring Security.

**Audit & notifications** - administrative changes are written to `audit_logs`; extraction and
other events create in-app notifications.

---

## 3. Project structure

```
skill-gap-dashboard/
├── pom.xml
├── application.properties.example      # copy of the config template (no real secrets)
├── README.md
└── src
    ├── main
    │   ├── java/com/skillgap
    │   │   ├── config/        # SecurityConfig, AppProperties, DataSeeder
    │   │   ├── controller/    # thin Spring MVC controllers
    │   │   ├── service/       # business logic incl. the gap engine
    │   │   ├── repository/    # Spring Data JPA repositories
    │   │   ├── entity/        # 26 JPA entities + enums
    │   │   ├── dto/           # validated form/backing objects
    │   │   ├── security/      # UserDetailsService, current-user resolver, throttle
    │   │   ├── exception/     # global handler, friendly errors
    │   │   └── util/          # skill normalizer, UI helpers
    │   └── resources
    │       ├── application.properties        # reads DB_URL/DB_USERNAME/DB_PASSWORD
    │       ├── static/ (css, js, vendor)     # Bootstrap 5 + Chart.js bundled
    │       └── templates/                    # Thymeleaf pages
    └── test                  # 28 JUnit 5 + MockMvc tests (H2, no MySQL needed)
```

---

## 4. Run it locally on Windows (step by step)

### 4.1 Install the JDK (21) and Maven

Option A - winget (Windows 10/11 terminal, run as normal user):

```powershell
winget install Microsoft.OpenJDK.21
winget install Apache.Maven
```

Option B - manual download:
1. JDK 21: <https://adoptium.net/> -> Temurin 21 (x64 MSI), accept defaults.
2. Maven: <https://maven.apache.org/download.cgi> -> `binary zip`, unzip to `C:\tools\maven`.

Then set the environment variables (System Properties -> Environment Variables, or PowerShell):

```powershell
# JDK
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.x.x-hotspot", "User")
# Maven (only if you used the zip)
[Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\tools\maven", "User")
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\tools\maven\bin", "User")
```

Open a **new** terminal and verify:

```powershell
java -version    # openjdk 21.x
mvn -version     # Apache Maven 3.9.x
```

### 4.2 Install MySQL 8 and create the database

1. Download MySQL Community Server 8.x from <https://dev.mysql.com/downloads/mysql/> (or install
   via winget: `winget install Oracle.MySQL`).
2. During setup remember the root password you choose.
3. Create the database (MySQL shell or any client):

```sql
CREATE DATABASE skillgap CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4.3 Configure the database credentials

The app reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from the environment and falls back to
`root/root` on `localhost:3306/skillgap`. **Do not commit real passwords.**

PowerShell (current user, persists):

```powershell
[Environment]::SetEnvironmentVariable("DB_URL",    "jdbc:mysql://localhost:3306/skillgap?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8", "User")
[Environment]::SetEnvironmentVariable("DB_USERNAME","root", "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD","YOUR_MYSQL_PASSWORD", "User")
```

Alternatively edit `src/main/resources/application.properties` locally (that file is only a
local override target in your checkout - keep real passwords out of Git) or copy
`application.properties.example` as a guide.

### 4.4 Build and run the application

```powershell
cd skill-gap-dashboard
mvn spring-boot:run
```

First start creates all 26 tables (Hibernate `ddl-auto=update`) and seeds catalog data:
categories, skills, 7 job roles with levels/weights, 4 sample assessments, learning resources
and the demo accounts.

Open a browser at <http://localhost:8080/> - you are redirected to the login page.

Stop the server with `Ctrl+C`.

### 4.5 Create an account and test the main features

1. **Register** a new account (or use the clearly labelled demo accounts):
   - user: `demo@example.com` / `Demo@123`
   - admin: `admin@example.com` / `Admin@123`
2. **My Skills** -> add a few skills with levels (e.g. Java 3, SQL 2, HTML 3).
3. **Resume & Documents** -> upload a PDF/DOCX resume that mentions skills -> review the
   detected skills -> confirm or reject each one.
4. **Job Roles** -> open *Java Developer* -> **Skill Gap Analysis** -> run the analysis.
5. **Work Specifications** -> paste a work assignment -> confirm the detected requirements ->
   run the analysis.
6. **Dashboard** -> check alignment %, charts, gaps and roadmap progress (all live data).
7. **Assessments** -> take *Java Basics* -> apply the score to your profile.
8. **Learning Roadmap** -> generate a plan from your analysis -> complete a task -> watch the
   progress bar on the Dashboard move.
9. **Reports** -> open the report -> Print / Save as PDF.
10. Log out, log in as `admin@example.com` -> **Admin Panel** -> add a skill or a role
    requirement, then log back in as a normal user: the Admin link is hidden *and* the URLs
    return 403 for non-admins.

### 4.6 Run the tests

```powershell
mvn test
```

Tests use an in-memory H2 database - no MySQL server required. 28 tests cover registration,
login, authorization, skill CRUD and duplicate prevention, PDF/DOCX extraction, invalid/scanned
file handling, evidence confirm/reject, gap/alignment formulas, priority rules, unassessed
skills, work-spec -> analysis -> report -> roadmap -> dashboard flow, assessment grading, report
access restrictions and page rendering.

---

## 5. The gap formulas

```
Gap            = max(0, Required Level - Current Level)
Gap Percentage = Gap / Required Level x 100

Alignment (%)  = 100 x SUM(weight x min(current, required) / required)
                    / SUM(all weights)          (REQUIRED skills only)
```

* Unassessed skills count as **level 0** for the calculation but are reported separately with an
  explicit explanation - *unassessed does not mean the person has no ability*.
* Priority rules (deterministic): `score = weight x gap`
  - `MET` when gap = 0
  - `CRITICAL` when gap >= 3 and weight >= 4, or score >= 12
  - `HIGH` when score >= 6, `MEDIUM` when score >= 3, otherwise `LOW`
* The alignment percentage is a study-planning estimate, **not** a guarantee of employability.

---

## 6. Database tables

`users`, `user_profiles`, `skill_categories`, `skills`, `user_skills`, `job_roles`,
`job_role_skills`, `input_documents`, `skill_evidence`, `skill_extraction_runs`,
`analysis_sessions`, `analysis_requirements`, `gap_analyses`, `gap_analysis_details`,
`assessments`, `assessment_questions`, `assessment_options`, `assessment_attempts`,
`assessment_answers`, `learning_resources`, `learning_roadmaps`, `roadmap_tasks`,
`certifications`, `user_projects`, `notifications`, `audit_logs`

Schema is created and kept up to date from the JPA entities (`spring.jpa.hibernate.ddl-auto=update`)
with primary keys, unique constraints (e.g. user email, skill normalized name, one skill per user),
foreign keys and indexes on the hot columns (user id, role id, document id, ...).

---

## 7. Security notes

- Passwords stored only as BCrypt hashes; never logged, never returned to the client
- Uploaded files live **outside the web root** (`./data/uploads`); only metadata is in MySQL and
  downloads are ownership-checked - no public file URLs
- Extracted document text is treated as untrusted data and only ever displayed as text
- CSRF enabled for all state-changing forms; ownership checks on every read/write route
- Server-side Bean Validation on all forms; file extension + size validation on uploads
- Database credentials come from environment variables (see `application.properties.example`)
- Global exception handler shows friendly pages; stack traces stay in the server log
- Administrative changes are written to `audit_logs`

---

## 8. Not implemented / known limitations

- **No OCR** - scanned/image-only PDFs fail gracefully with an explicit message
- Extraction is **rule-based** (catalog + alias keyword matching). The extractor is a small
  isolated service so an AI API can be plugged in later, but no paid API is used
- Login throttle is in-memory (single-instance only); no email verification or password reset
- "Report as PDF" uses the browser print dialog (print stylesheet), not a server-side PDF library
- Certifications, projects, work experience and mentor evaluations are stored as **evidence**;
  they never automatically raise a proficiency level
- Demo seed data contains only catalog/roles/assessments/resources and the two labelled demo
  accounts - **no fabricated scores, analyses or progress**

---

## 9. Quick command reference

```powershell
mvn test              # run all tests (H2, no MySQL needed)
mvn spring-boot:run   # start on http://localhost:8080
mvn -q package        # build the executable jar
java -jar target\skill-gap-dashboard-1.0.0.jar
```
