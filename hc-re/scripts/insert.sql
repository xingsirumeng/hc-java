-- 插入学生数据
INSERT INTO student (student_id, name, password) VALUES
('202201010001', 'Zhang Wei', 'password123'),
('202201010002', 'Li Na', 'password456'),
('202201010003', 'Wang Qiang', 'password789'),
('202202010001', 'Liu Yang', 'passwordabc'),
('202202010002', 'Chen Min', 'passworddef'),
('202303010001', 'Yang Jing', 'passwordghi'),
('202303010002', 'Zhao Lei', 'passwordjkl'),
('202304010001', 'Huang Yu', 'passwordmno');

-- 插入教师数据
INSERT INTO teacher (teacher_id, name, password) VALUES
('T001', 'Prof. Wang', 'teacherpass1'),
('T002', 'Assoc. Prof. Li', 'teacherpass2'),
('T003', 'Lecturer Zhang', 'teacherpass3'),
('T004', 'Prof. Liu', 'teacherpass4');

-- 插入课程数据
INSERT INTO courses (course_id, course_name, teacher_id, credit) VALUES
('CS101', 'Data Structures and Algorithms', 'T001', 3),
('CS102', 'Operating Systems', 'T001', 3),
('CS103', 'Database Principles', 'T002', 3),
('SE101', 'Introduction to Software Engineering', 'T002', 2),
('SE102', 'Web Development Technologies', 'T003', 2),
('DS101', 'Fundamentals of Data Analysis', 'T003', 3),
('DS102', 'Introduction to Machine Learning', 'T004', 3),
('AI101', 'Introduction to Artificial Intelligence', 'T004', 2);

-- 插入作业数据
INSERT INTO assignment (name, start_time, end_time, course_id) VALUES
('DS Homework 1', '2026-06-01 08:00:00+08', '2026-06-15 23:59:00+08', 'CS101'),
('DS Homework 2', '2026-06-20 08:00:00+08', '2026-07-05 23:59:00+08', 'CS101'),
('DB Homework 1', '2026-06-05 08:00:00+08', '2026-06-20 23:59:00+08', 'CS103'),
('OS Lab 1', '2026-06-10 08:00:00+08', '2026-06-25 23:59:00+08', 'CS102'),
('SE Practice', '2026-06-15 08:00:00+08', '2026-07-10 23:59:00+08', 'SE101'),
('Web Project', '2026-06-01 08:00:00+08', '2026-06-30 23:59:00+08', 'SE102'),
('DA Report', '2026-06-08 08:00:00+08', '2026-06-22 23:59:00+08', 'DS101'),
('ML Homework 1', '2026-06-12 08:00:00+08', '2026-06-26 23:59:00+08', 'DS102'),
('AI Project', '2026-06-18 08:00:00+08', '2026-07-15 23:59:00+08', 'AI101');

-- 插入选课数据
INSERT INTO sc (student_id, course_id) VALUES
('202201010001', 'CS101'),
('202201010001', 'CS102'),
('202201010001', 'CS103'),
('202201010002', 'CS101'),
('202201010002', 'CS102'),
('202201010003', 'CS101'),
('202201010003', 'CS103'),
('202202010001', 'SE101'),
('202202010001', 'SE102'),
('202202010002', 'SE101'),
('202202010002', 'CS101'),
('202303010001', 'DS101'),
('202303010001', 'DS102'),
('202303010001', 'CS101'),
('202303010002', 'DS101'),
('202303010002', 'CS103'),
('202304010001', 'AI101'),
('202304010001', 'CS102'),
('202304010001', 'DS102');

-- 插入提交记录数据
INSERT INTO submission (assignment_name, student_id, submission_time, filepath, original_filename) VALUES
