package jxnu.hc_re.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.service.AssignmentService;
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

@RestController
@RequestMapping("/assignment")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    /** 获取某课程的作业列表（仅该教师的课程） */
    @GetMapping
    public Result<List<Assignment>> getAssignments(
            @RequestParam String courseId,
            HttpServletRequest request) {
        String teacherId = (String) request.getAttribute("currentTeacherId");
        List<Assignment> list = assignmentService.getAssignmentsByCourse(courseId, teacherId);
        return Result.success(list);
    }

    /** 布置新作业 */
    @PostMapping
    public Result<String> createAssignment(@RequestBody Assignment assignment) {
        if (assignmentService.findByName(assignment.getName()) != null) {
            return Result.error("作业名称已存在");
        }
        assignmentService.createAssignment(assignment);
        return Result.success("作业布置成功");
    }

    /** 修改作业 */
    @PutMapping("/{assignmentName}")
    public Result<String> updateAssignment(@PathVariable String assignmentName,
                                           @RequestBody Assignment assignment) {
        assignment.setName(assignmentName);
        assignmentService.updateAssignment(assignment);
        return Result.success("作业修改成功");
    }

    /** 删除作业 */
    @DeleteMapping("/{assignmentName}")
    public Result<String> deleteAssignment(@PathVariable String assignmentName) {
        assignmentService.deleteAssignment(assignmentName);
        return Result.success("作业删除成功");
    }

    /** 查看某作业的所有提交 */
    @GetMapping("/{assignmentName}/submissions")
    public Result<List<Submission>> getSubmissions(@PathVariable String assignmentName) {
        List<Submission> submissions = assignmentService.getSubmissions(assignmentName);
        return Result.success(submissions);
    }
}
