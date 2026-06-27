package jxnu.hc_re.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jxnu.hc_re.mapper.TeacherMapper;
import jxnu.hc_re.mapper.AssignmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.Course;
import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.pojo.Sc;
import jxnu.hc_re.pojo.Student;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.pojo.Teacher;
import jxnu.hc_re.service.AssignmentService;
import jxnu.hc_re.service.CourseService;
import jxnu.hc_re.service.ScService;
import jxnu.hc_re.service.StudentService;
import jxnu.hc_re.service.SubmissionService;
import jxnu.hc_re.service.TeacherService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private ScService scService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    // ================================================================
    // 统计信息
    // ================================================================

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("studentCount", studentService.findAll().size());
        stats.put("teacherCount", teacherService.findAllTeachers().size());
        stats.put("courseCount", courseService.findAll().size());
        stats.put("assignmentCount", assignmentService.findAll().size());
        stats.put("submissionCount", submissionService.findAll().size());
        stats.put("enrollmentCount", scService.findAll().size());
        return Result.success(stats);
    }

    // ================================================================
    // 学生管理
    // ================================================================

    @GetMapping("/students")
    public Result<List<Student>> listStudents() {
        return Result.success(studentService.findAll());
    }

    @GetMapping("/students/{studentId}")
    public Result<Student> getStudent(@PathVariable String studentId) {
        Student student = studentService.findById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        return Result.success(student);
    }

    @PostMapping("/students")
    public Result<String> addStudent(@RequestBody Student student) {
        if (student.getStudentId() == null || student.getStudentId().isBlank()) {
            return Result.error("学号不能为空");
        }
        if (student.getName() == null || student.getName().isBlank()) {
            return Result.error("姓名不能为空");
        }
        if (student.getPassword() == null || student.getPassword().isBlank()) {
            return Result.error("密码不能为空");
        }
        if (studentService.findById(student.getStudentId()) != null) {
            return Result.error("该学号已存在");
        }
        studentService.addStudent(student);
        return Result.success("学生添加成功");
    }

    @PutMapping("/students/{studentId}")
    public Result<String> updateStudent(@PathVariable String studentId, @RequestBody Student student) {
        Student existing = studentService.findById(studentId);
        if (existing == null) {
            return Result.error("学生不存在");
        }
        student.setStudentId(studentId);
        studentService.updateStudent(student);
        return Result.success("学生更新成功");
    }

    @DeleteMapping("/students/{studentId}")
    public Result<String> deleteStudent(@PathVariable String studentId) {
        Student existing = studentService.findById(studentId);
        if (existing == null) {
            return Result.error("学生不存在");
        }
        try {
            studentService.deleteStudent(studentId);
            return Result.success("学生删除成功");
        } catch (Exception e) {
            return Result.error("删除失败，该学生可能有关联的提交记录或选课记录");
        }
    }

    // ================================================================
    // 教师管理
    // ================================================================

    @GetMapping("/teachers")
    public Result<List<Teacher>> listTeachers() {
        return Result.success(teacherService.findAllTeachers());
    }

    @GetMapping("/teachers/{teacherId}")
    public Result<Teacher> getTeacher(@PathVariable String teacherId) {
        Teacher teacher = teacherService.findById(teacherId);
        if (teacher == null) {
            return Result.error("教师不存在");
        }
        return Result.success(teacher);
    }

    @PostMapping("/teachers")
    public Result<String> addTeacher(@RequestBody Teacher teacher) {
        if (teacher.getTeacherId() == null || teacher.getTeacherId().isBlank()) {
            return Result.error("教师ID不能为空");
        }
        if (teacher.getName() == null || teacher.getName().isBlank()) {
            return Result.error("姓名不能为空");
        }
        if (teacher.getPassword() == null || teacher.getPassword().isBlank()) {
            return Result.error("密码不能为空");
        }
        if (teacherService.findById(teacher.getTeacherId()) != null) {
            return Result.error("该教师ID已存在");
        }
        teacherService.addTeacher(teacher);
        return Result.success("教师添加成功");
    }

    @PutMapping("/teachers/{teacherId}")
    public Result<String> updateTeacher(@PathVariable String teacherId, @RequestBody Teacher teacher) {
        Teacher existing = teacherService.findById(teacherId);
        if (existing == null) {
            return Result.error("教师不存在");
        }
        teacher.setTeacherId(teacherId);
        teacherService.updateTeacher(teacher);
        return Result.success("教师更新成功");
    }

    @DeleteMapping("/teachers/{teacherId}")
    public Result<String> deleteTeacher(@PathVariable String teacherId) {
        Teacher existing = teacherService.findById(teacherId);
        if (existing == null) {
            return Result.error("教师不存在");
        }
        try {
            teacherService.deleteTeacher(teacherId);
            return Result.success("教师删除成功");
        } catch (Exception e) {
            return Result.error("删除失败，该教师可能有关联的课程");
        }
    }

    // ================================================================
    // 课程管理
    // ================================================================

    @GetMapping("/courses")
    public Result<List<Course>> listCourses() {
        return Result.success(courseService.findAll());
    }

    @GetMapping("/courses/{courseId}")
    public Result<Course> getCourse(@PathVariable String courseId) {
        Course course = courseService.findById(courseId);
        if (course == null) {
            return Result.error("课程不存在");
        }
        return Result.success(course);
    }

    @PostMapping("/courses")
    public Result<String> addCourse(@RequestBody Course course) {
        if (course.getCourseId() == null || course.getCourseId().isBlank()) {
            return Result.error("课程ID不能为空");
        }
        if (course.getCourseName() == null || course.getCourseName().isBlank()) {
            return Result.error("课程名不能为空");
        }
        if (courseService.findById(course.getCourseId()) != null) {
            return Result.error("该课程ID已存在");
        }
        courseService.addCourse(course);
        return Result.success("课程添加成功");
    }

    @PutMapping("/courses/{courseId}")
    public Result<String> updateCourse(@PathVariable String courseId, @RequestBody Course course) {
        Course existing = courseService.findById(courseId);
        if (existing == null) {
            return Result.error("课程不存在");
        }
        course.setCourseId(courseId);
        courseService.updateCourse(course);
        return Result.success("课程更新成功");
    }

    @DeleteMapping("/courses/{courseId}")
    public Result<String> deleteCourse(@PathVariable String courseId) {
        Course existing = courseService.findById(courseId);
        if (existing == null) {
            return Result.error("课程不存在");
        }
        try {
            courseService.deleteCourse(courseId);
            return Result.success("课程删除成功");
        } catch (Exception e) {
            return Result.error("删除失败，该课程可能有关联的作业或选课记录");
        }
    }

    // ================================================================
    // 作业管理
    // ================================================================

    @GetMapping("/assignments")
    public Result<List<Assignment>> listAssignments(@RequestParam(required = false) String courseId) {
        if (courseId != null && !courseId.isBlank()) {
            List<Assignment> list = assignmentMapper.findByCourseId(courseId);
            return Result.success(list);
        }
        return Result.success(assignmentService.findAll());
    }

    @GetMapping("/assignments/{name}")
    public Result<Assignment> getAssignment(@PathVariable String name) {
        Assignment assignment = assignmentService.findByName(name);
        if (assignment == null) {
            return Result.error("作业不存在");
        }
        return Result.success(assignment);
    }

    @PostMapping("/assignments")
    public Result<String> addAssignment(@RequestBody Assignment assignment) {
        if (assignment.getName() == null || assignment.getName().isBlank()) {
            return Result.error("作业名称不能为空");
        }
        if (assignmentService.findByName(assignment.getName()) != null) {
            return Result.error("作业名称已存在");
        }
        assignmentService.createAssignment(assignment);
        return Result.success("作业添加成功");
    }

    @PutMapping("/assignments/{name}")
    public Result<String> updateAssignment(@PathVariable String name, @RequestBody Assignment assignment) {
        Assignment existing = assignmentService.findByName(name);
        if (existing == null) {
            return Result.error("作业不存在");
        }
        assignment.setName(name);
        assignmentService.updateAssignment(assignment);
        return Result.success("作业更新成功");
    }

    @DeleteMapping("/assignments/{name}")
    public Result<String> deleteAssignment(@PathVariable String name) {
        Assignment existing = assignmentService.findByName(name);
        if (existing == null) {
            return Result.error("作业不存在");
        }
        try {
            assignmentService.deleteAssignment(name);
            return Result.success("作业删除成功");
        } catch (Exception e) {
            return Result.error("删除失败，该作业可能有关联的提交记录");
        }
    }

    // ================================================================
    // 提交记录管理
    // ================================================================

    @GetMapping("/submissions")
    public Result<List<Submission>> listSubmissions(
            @RequestParam(required = false) String assignmentName,
            @RequestParam(required = false) String studentId) {
        List<Submission> all = submissionService.findAll();
        if (assignmentName != null && !assignmentName.isBlank()) {
            all = all.stream()
                    .filter(s -> assignmentName.equals(s.getAssignmentName()))
                    .collect(Collectors.toList());
        }
        if (studentId != null && !studentId.isBlank()) {
            all = all.stream()
                    .filter(s -> studentId.equals(s.getStudentId()))
                    .collect(Collectors.toList());
        }
        return Result.success(all);
    }

    @DeleteMapping("/submissions")
    public Result<String> deleteSubmission(@RequestBody Map<String, String> body) {
        String assignmentName = body.get("assignmentName");
        String studentId = body.get("studentId");
        if (assignmentName == null || studentId == null) {
            return Result.error("请提供作业名称和学生ID");
        }
        submissionService.deleteSubmission(assignmentName, studentId);
        return Result.success("提交记录删除成功");
    }

    /** 批量删除提交记录 */
    @SuppressWarnings("unchecked")
    @DeleteMapping("/submissions/batch")
    public Result<String> batchDeleteSubmissions(@RequestBody Map<String, Object> body) {
        List<Map<String, String>> items = (List<Map<String, String>>) body.get("submissions");
        if (items == null || items.isEmpty()) {
            return Result.error("请提供要删除的提交记录列表");
        }
        int count = 0;
        for (Map<String, String> item : items) {
            String assignmentName = item.get("assignmentName");
            String studentId = item.get("studentId");
            if (assignmentName != null && studentId != null) {
                submissionService.deleteSubmission(assignmentName, studentId);
                count++;
            }
        }
        return Result.success("批量删除成功，共删除 " + count + " 条提交记录");
    }

    // ================================================================
    // 选课管理
    // ================================================================

    @GetMapping("/enrollments")
    public Result<List<Sc>> listEnrollments(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String studentId) {
        if (courseId != null && !courseId.isBlank()) {
            return Result.success(scService.findByCourseId(courseId));
        }
        if (studentId != null && !studentId.isBlank()) {
            return Result.success(scService.findByStudentId(studentId));
        }
        return Result.success(scService.findAll());
    }

    @PostMapping("/enrollments")
    public Result<String> addEnrollment(@RequestBody Map<String, String> body) {
        String studentId = body.get("studentId");
        String courseId = body.get("courseId");
        if (studentId == null || studentId.isBlank()) {
            return Result.error("学生ID不能为空");
        }
        if (courseId == null || courseId.isBlank()) {
            return Result.error("课程ID不能为空");
        }
        // 检查是否已存在
        List<Sc> existing = scService.findByStudentId(studentId);
        boolean alreadyEnrolled = existing.stream()
                .anyMatch(sc -> courseId.equals(sc.getCourseId()));
        if (alreadyEnrolled) {
            return Result.error("该学生已选修此课程");
        }
        scService.enroll(studentId, courseId);
        return Result.success("选课成功");
    }

    @DeleteMapping("/enrollments/{scId}")
    public Result<String> deleteEnrollment(@PathVariable Integer scId) {
        scService.unenroll(scId);
        return Result.success("退课成功");
    }
}
