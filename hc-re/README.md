# hc-re — Homework Correction & Review System

A Spring Boot + MyBatis backend for managing course assignments, student submissions, AI-assisted grading, and AIGC detection. Built for university-level teaching workflows where teachers create assignments across multiple classes/courses and students submit work for review.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.1 (Java 21) |
| Web | Spring Web MVC |
| ORM | MyBatis (annotation-based) |
| Database | PostgreSQL (prod), MySQL (driver included) |
| Auth | JWT (jjwt 0.12.6) + BCrypt |
| AI Integration | REST calls to a companion Python service |
| Build | Maven |
| Util | Lombok |

## Architecture

```
src/main/java/jxnu/hc_re/
├── config/              # JWT utils, interceptor, WebMvc config
├── controller/          # REST controllers
│   ├── LoginController          # Teacher login (JWT)
│   ├── TeacherController        # Teacher CRUD
│   ├── StudentController        # Student queries (by class)
│   ├── StudentMockController    # Student mock endpoints (no auth, for testing)
│   ├── ClassesController        # Classes by teacher
│   ├── AssignmentController     # Assignment CRUD + submissions
│   ├── SubmissionController     # Manual grading (score + comment)
│   ├── FileController           # Upload / download / preview
│   └── AiController             # AI grading + AIGC detection
├── pojo/                # Data objects (entity + dto)
├── mapper/              # MyBatis mapper interfaces
├── service/             # Service interfaces + impls
└── HcReApplication.java # Entry point
```

## Database Schema

7 tables modeling the university assignment workflow:

```
teacher ──→ courses ──→ assignment ──→ submission
                              │              │
                          classes         student
                              │              │
                              └── sc ────────┘
```

### Tables

| Table | Description | Key columns |
|---|---|---|
| `teacher` | Teachers | `teacher_id`, `name`, `password` (BCrypt) |
| `student` | Students | `student_id` (12-char), `name`, `password` (plaintext) |
| `classes` | Classes/cohorts | `class_id`, `class_name`, `grade`, `major` |
| `courses` | Courses taught by teachers | `course_id`, `course_name`, `teacher_id`, `credit` |
| `assignment` | Homework assignments | `name` (PK), `start_time`, `end_time`, `class_id`, `course_id` |
| `sc` | Student-Course enrollment | `sc_id` (serial), `student_id`, `course_id` (unique pair) |
| `submission` | Student submissions | `assignment_name` + `student_id` (composite PK), `filepath`, `score`, `comment`, `aigc_score` |

## API Endpoints

> Base path: `/api` (configured in `application.yml`).  
> All endpoints except `/login`, `/hello`, `/student-mock/**`, and `/file/upload` require `Authorization: Bearer <token>`.

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/login` | No | Teacher login — returns JWT token + teacher info |
| POST | `/login/student` | No | Student login — returns JWT token + student info |

### Teacher Management

| Method | Path | Description |
|---|---|---|
| GET | `/teacher` | List all teachers |
| POST | `/teacher/add` | Add a teacher (password auto-BCrypt) |
| GET | `/teacher/{teacherId}` | Get teacher by ID |
| PUT | `/teacher/{teacherId}` | Update teacher |
| DELETE | `/teacher/{teacherId}` | Delete teacher |

### Student (admin/teacher view)

| Method | Path | Description |
|---|---|---|
| GET | `/student/by-class/{classId}` | Get students enrolled in a class |

### Student (self-service, requires student JWT)

| Method | Path | Description |
|---|---|---|
| GET | `/student/profile` | Get own profile (studentId, name — no password) |
| PUT | `/student/profile` | Update name and/or password |
| GET | `/student/courses` | Enrolled courses |
| GET | `/student/classes` | Enrolled classes |
| GET | `/student/assignments` | All assignments with submission status + grades |
| GET | `/student/assignments/{name}` | Single assignment detail with submission status |
| POST | `/student/assignments/{name}/submit` | Submit homework (body: `filepath`, `originalFilename`) |
| GET | `/student/submissions` | All my submissions |
| GET | `/student/submissions/{name}` | Specific submission detail |
| GET | `/student/grades` | Graded submissions only | |

### Classes

| Method | Path | Description |
|---|---|---|
| GET | `/classes/my` | Get classes taught by the current teacher |

### Assignments

| Method | Path | Description |
|---|---|---|
| GET | `/assignment?classId={id}` | Get assignments for a class (only current teacher's courses) |
| POST | `/assignment` | Create a new assignment |
| GET | `/assignment/{name}/submissions` | Get all submissions for an assignment |

### Submissions & Grading

| Method | Path | Description |
|---|---|---|
| PUT | `/submission/grade` | Manual grading: set `score` + `comment` |

### File Operations

| Method | Path | Description |
|---|---|---|
| POST | `/file/upload` | Upload a file (returns `filepath` + `originalFilename`) |
| GET | `/file/download?filepath=...` | Download a file |
| GET | `/file/preview?filepath=...` | Preview a file inline |

### AI Grading & AIGC Detection

| Method | Path | Description |
|---|---|---|
| POST | `/ai/grade` | AI grades a submission file against requirements |
| POST | `/ai/aigc-detect` | Detect AI-generated content in raw text |
| POST | `/ai/aigc-detect-file` | Detect AI-generated content in a file, persist result to DB |

### Student Mock (no auth, for testing)

| Method | Path | Description |
|---|---|---|
| GET | `/student-mock/assignments` | List all assignments |
| GET | `/student-mock/students` | List all students |
| POST | `/student-mock/submit` | Submit homework (creates or overwrites) |

## Quick Start

### Prerequisites

- Java 21+
- PostgreSQL (or MySQL)
- Maven 3.8+

### Setup

```bash
# 1. Clone and enter the project
cd hc-re

# 2. Create the database
psql -U postgres -c "CREATE DATABASE hc;"

# 3. Run schema + seed data
psql -U postgres -d hc -f scripts/table.sql
psql -U postgres -d hc -f scripts/insert.sql

# 4. Configure database connection in src/main/resources/application.yml
#    (default: localhost:5432, user: postgres, password: 123321, db: hc)

# 5. Build and run
./mvnw spring-boot:run

# 6. Test
curl http://localhost:8080/api/hello
# → "Hello World"

# 7. Login as a teacher
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"teacherId":"T001","password":"teacherpass1"}'
# → returns JWT token
```

## Configuration

Key settings in `application.yml`:

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/hc` | Database URL (PostgreSQL) |
| `server.servlet.context-path` | `/api` | API prefix |
| `jwt.secret` | `hc-re-teacher-login-secret-key-2026` | JWT signing key (**change in production**) |
| `jwt.expiration` | `86400000` | Token expiry in ms (24 hours) |
| `upload.path` | `./uploads` | File upload directory |

## AI Service

The backend calls a companion **Python AI service** running on `http://localhost:8000`:

| AI Endpoint | Purpose |
|---|---|
| `POST /api/v1/grade/upload` | Grade a student file against assignment requirements |
| `POST /api/v1/aigc/detect` | Score raw text for AI-generated likelihood |
| `POST /api/v1/aigc/detect/upload` | Score a file for AI-generated likelihood |

Without the AI service running, AI endpoints will return connection errors; the rest of the system remains functional.

## Project Status

- ✅ Teacher login (JWT + BCrypt, automatic plaintext-password upgrade)
- ✅ Teacher CRUD
- ✅ Student query by class
- ✅ Class management (teacher-scoped)
- ✅ Assignment CRUD (with `requirement` field)
- ✅ File upload / download / preview (with path traversal protection)
- ✅ Submission CRUD (upsert on conflict)
- ✅ Manual grading (score + comment)
- ✅ AI-assisted grading
- ✅ AIGC text & file detection
- ✅ Student login (JWT + BCrypt, auto-upgrade)
- ✅ Student profile (view & update)
- ✅ Student's courses / classes
- ✅ Student's assignments (with submission status)
- ✅ Student submit homework (with deadline validation)
- ✅ Student submission history & grades
- ✅ Role-based path authorization (teacher vs student)
