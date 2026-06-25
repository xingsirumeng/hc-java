package jxnu.hc_re.controller;

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

    /** 批改作业：打分 + 评语 + 标注文件 */
    @PutMapping("/grade")
    public Result<String> grade(@RequestBody Map<String, Object> body) {
        String assignmentName = (String) body.get("assignmentName");
        String studentId = (String) body.get("studentId");
        Integer score = body.get("score") != null ? ((Number) body.get("score")).intValue() : null;
        String comment = (String) body.get("comment");
        String annotatedFilepath = (String) body.get("annotatedFilepath");

        submissionService.grade(assignmentName, studentId, score, comment, annotatedFilepath);
        return Result.success("批改成功");
    }
}
