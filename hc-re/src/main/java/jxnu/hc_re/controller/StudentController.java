package jxnu.hc_re.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jxnu.hc_re.mapper.CourseMapper;
import jxnu.hc_re.pojo.Course;
import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.pojo.Student;
import jxnu.hc_re.pojo.StudentAssignmentDTO;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.service.AssignmentService;
import jxnu.hc_re.service.StudentService;
import jxnu.hc_re.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private CourseMapper courseMapper;

    // ──────────────────────────────────────────────
    // 学生自身操作（需要学生 JWT）
    // ──────────────────────────────────────────────

    /** 获取当前学生个人资料 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        Student student = studentService.findById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        Map<String, Object> profile = Map.of(
                "studentId", student.getStudentId(),
                "name", student.getName());
        return Result.success(profile);
    }

    /** 更新当前学生个人资料（姓名 / 密码） */
    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        String name = (String) body.get("name");
        String password = (String) body.get("password");

        if ((name == null || name.isBlank()) && (password == null || password.isBlank())) {
            return Result.error("至少需要提供姓名或密码");
        }

        studentService.updateProfile(studentId, name, password);
        return Result.success("资料更新成功");
    }

    /** 获取当前学生选修的课程列表 */
    @GetMapping("/courses")
    public Result<List<Course>> getMyCourses(HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        List<Course> courses = courseMapper.findByStudentId(studentId);
        return Result.success(courses);
    }

    /** 获取当前学生的所有作业（含提交状态和成绩） */
    @GetMapping("/assignments")
    public Result<List<StudentAssignmentDTO>> getMyAssignments(HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        List<StudentAssignmentDTO> list = assignmentService.getStudentAssignments(studentId);
        return Result.success(list);
    }

    /** 获取某个作业的详情（含提交状态） */
    @GetMapping("/assignments/{assignmentName}")
    public Result<StudentAssignmentDTO> getAssignmentDetail(
            @PathVariable String assignmentName,
            HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        StudentAssignmentDTO detail = assignmentService.getStudentAssignmentDetail(assignmentName, studentId);
        if (detail == null) {
            return Result.error("作业不存在或你未选修该课程");
        }
        return Result.success(detail);
    }

    /** 提交作业 */
    @PostMapping("/assignments/{assignmentName}/submit")
    public Result<String> submitAssignment(
            @PathVariable String assignmentName,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        String filepath = (String) body.get("filepath");
        String originalFilename = (String) body.get("originalFilename");

        if (filepath == null || filepath.isBlank()) {
            return Result.error("请先上传文件");
        }

        try {
            submissionService.submit(assignmentName, studentId, filepath, originalFilename);
            return Result.success("提交成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取当前学生的所有提交记录 */
    @GetMapping("/submissions")
    public Result<List<Submission>> getMySubmissions(HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        List<Submission> list = submissionService.getByStudentId(studentId);
        return Result.success(list);
    }

    /** 获取当前学生某个作业的提交详情 */
    @GetMapping("/submissions/{assignmentName}")
    public Result<Submission> getSubmissionDetail(
            @PathVariable String assignmentName,
            HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        Submission submission = submissionService.getByStudentAndAssignment(assignmentName, studentId);
        if (submission == null) {
            return Result.error("未找到该作业的提交记录");
        }
        return Result.success(submission);
    }

    /** 获取当前学生已出分的作业 */
    @GetMapping("/grades")
    public Result<List<Submission>> getMyGrades(HttpServletRequest request) {
        String studentId = (String) request.getAttribute("currentStudentId");
        List<Submission> all = submissionService.getByStudentId(studentId);
        List<Submission> graded = all.stream()
                .filter(s -> s.getScore() != null)
                .collect(Collectors.toList());
        return Result.success(graded);
    }

    // ──────────────────────────────────────────────
    // 教师端操作（需要教师 JWT，由拦截器保证）
    // ──────────────────────────────────────────────

    /** 根据课程 ID 获取学生列表（教师端） */
    @GetMapping("/by-course/{courseId}")
    public Result<List<Student>> getStudentsByCourse(@PathVariable String courseId) {
        List<Student> list = studentService.getStudentsByCourseId(courseId);
        return Result.success(list);
    }
}
