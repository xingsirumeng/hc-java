package jxnu.hc_re.controller;

import java.util.List;
import java.util.Map;

import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.pojo.Student;
import jxnu.hc_re.service.AssignmentService;
import jxnu.hc_re.service.StudentService;
import jxnu.hc_re.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟学生端（无需登录，用于测试提交作业）
 */
@RestController
@RequestMapping("/student-mock")
public class StudentMockController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SubmissionService submissionService;

    @GetMapping("/assignments")
    public Result<List<Assignment>> getAllAssignments() {
        return Result.success(assignmentService.findAll());
    }

    @GetMapping("/students")
    public Result<List<Student>> getAllStudents() {
        return Result.success(studentService.findAll());
    }

    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Map<String, Object> body) {
        String assignmentName = (String) body.get("assignmentName");
        String studentId = (String) body.get("studentId");
        String filepath = (String) body.get("filepath");
        String originalFilename = (String) body.get("originalFilename");

        boolean existed = submissionService.getByStudentAndAssignment(assignmentName, studentId) != null;
        submissionService.submit(assignmentName, studentId, filepath, originalFilename);
        return Result.success(existed ? "已覆盖之前的提交，评分已重置" : "提交成功");
    }
}
