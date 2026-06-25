CREATE TABLE IF NOT EXISTS student (
    student_id CHAR(12) NOT NULL,
    name VARCHAR(50) NOT NULL,
    password TEXT NOT NULL,
    PRIMARY KEY (student_id)
);

CREATE TABLE IF NOT EXISTS teacher (
    teacher_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    password TEXT NOT NULL
);

CREATE TABLE courses (
    course_id TEXT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    teacher_id TEXT NOT NULL,
    credit INT DEFAULT 2,
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)
);

CREATE TABLE assignment (
    name VARCHAR(50) NOT NULL PRIMARY KEY,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    course_id TEXT NOT NULL,
    requirement TEXT,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);

CREATE TABLE sc (
    sc_id SERIAL PRIMARY KEY,
    student_id CHAR(12) NOT NULL,
    course_id TEXT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    UNIQUE (student_id, course_id)
);

CREATE TABLE IF NOT EXISTS submission (
    assignment_name VARCHAR(50) NOT NULL,
    student_id CHAR(12) NOT NULL,
    submission_time TIMESTAMPTZ NOT NULL,
    filepath VARCHAR(1024) NOT NULL,
    original_filename VARCHAR(1024) NOT NULL,
    score INT DEFAULT NULL,
    comment TEXT DEFAULT NULL,
    aigc_score INT DEFAULT NULL,
    annotated_filepath VARCHAR(1024) DEFAULT NULL,
    PRIMARY KEY (assignment_name, student_id),
    FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    FOREIGN KEY (assignment_name) REFERENCES assignment(name) ON DELETE CASCADE
);
