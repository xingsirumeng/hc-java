package jxnu.hc_re.controller;

import java.util.List;
import java.util.Map;

import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/submission")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    /** 批改作业：打分 + 评语 + 标注文件 + 附件 */
    @PutMapping("/grade")
    public Result<String> grade(@RequestBody Map<String, Object> body) {
        String assignmentName = (String) body.get("assignmentName");
        String studentId = (String) body.get("studentId");
        Integer score = body.get("score") != null ? ((Number) body.get("score")).intValue() : null;
        String comment = (String) body.get("comment");
        String annotatedFilepath = (String) body.get("annotatedFilepath");
        String gradeAttachments = (String) body.get("gradeAttachments");

        submissionService.grade(assignmentName, studentId, score, comment, annotatedFilepath, gradeAttachments);
        return Result.success("批改成功");
    }

    /** 打回作业：教师退回作业要求学生重做 */
    @PutMapping("/return")
    public Result<String> returnSubmission(@RequestBody Map<String, Object> body) {
        String assignmentName = (String) body.get("assignmentName");
        String studentId = (String) body.get("studentId");
        String reason = (String) body.get("reason");

        if (assignmentName == null || studentId == null) {
            return Result.error("请提供作业名称和学生ID");
        }

        try {
            submissionService.returnSubmission(assignmentName, studentId, reason);
            return Result.success("作业已打回");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 批量批改作业 */
    @SuppressWarnings("unchecked")
    @PutMapping("/batch-grade")
    public Result<String> batchGrade(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> gradeList = (List<Map<String, Object>>) body.get("grades");
        if (gradeList == null || gradeList.isEmpty()) {
            return Result.error("请提供批改列表");
        }

        try {
            submissionService.batchGrade(gradeList);
            return Result.success("批量批改成功，共处理 " + gradeList.size() + " 份作业");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
