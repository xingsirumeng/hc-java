package jxnu.hc_re.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jxnu.hc_re.mapper.DeadlineExtensionMapper;
import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.AssignmentSimilarity;
import jxnu.hc_re.pojo.DeadlineExtension;
import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.service.AssignmentService;
import jxnu.hc_re.service.SimilarityService;
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

    @Autowired
    private DeadlineExtensionMapper deadlineExtensionMapper;

    @Autowired
    private SimilarityService similarityService;

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

    /** 批量删除作业 */
    @SuppressWarnings("unchecked")
    @DeleteMapping("/batch")
    public Result<String> batchDeleteAssignments(@RequestBody Map<String, Object> body) {
        List<String> names = (List<String>) body.get("names");
        if (names == null || names.isEmpty()) {
            return Result.error("请提供要删除的作业名称列表");
        }
        try {
            assignmentService.batchDeleteAssignments(names);
            return Result.success("批量删除成功，共删除 " + names.size() + " 个作业");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 查看某作业的所有提交 */
    @GetMapping("/{assignmentName}/submissions")
    public Result<List<Submission>> getSubmissions(@PathVariable String assignmentName) {
        List<Submission> submissions = assignmentService.getSubmissions(assignmentName);
        return Result.success(submissions);
    }

    // ──────────────────────────────────────────────
    // 延期管理
    // ──────────────────────────────────────────────

    /** 查看某作业的所有延期记录 */
    @GetMapping("/{assignmentName}/extensions")
    public Result<List<DeadlineExtension>> getExtensions(@PathVariable String assignmentName) {
        List<DeadlineExtension> extensions = deadlineExtensionMapper.findByAssignmentName(assignmentName);
        return Result.success(extensions);
    }

    /** 为单个学生延长作业提交时间 */
    @PostMapping("/{assignmentName}/extend")
    public Result<String> extendDeadline(@PathVariable String assignmentName,
                                         @RequestBody Map<String, Object> body) {
        String studentId = (String) body.get("studentId");
        String extendedEndTimeStr = (String) body.get("extendedEndTime");
        String reason = (String) body.get("reason");

        if (studentId == null || studentId.isBlank()) {
            return Result.error("请提供学生ID");
        }
        if (extendedEndTimeStr == null || extendedEndTimeStr.isBlank()) {
            return Result.error("请提供延长后的截止时间");
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date extendedEndTime = sdf.parse(extendedEndTimeStr);
            deadlineExtensionMapper.upsert(assignmentName, studentId, extendedEndTime, reason);
            return Result.success("延期设置成功");
        } catch (java.text.ParseException e) {
            return Result.error("日期格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        }
    }

    /** 批量为学生延长作业提交时间 */
    @SuppressWarnings("unchecked")
    @PostMapping("/{assignmentName}/extend/batch")
    public Result<String> batchExtendDeadline(@PathVariable String assignmentName,
                                              @RequestBody Map<String, Object> body) {
        List<Map<String, String>> extensions = (List<Map<String, String>>) body.get("extensions");
        if (extensions == null || extensions.isEmpty()) {
            return Result.error("请提供延期列表");
        }

        int count = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            for (Map<String, String> item : extensions) {
                String studentId = item.get("studentId");
                String extendedEndTimeStr = item.get("extendedEndTime");
                String reason = item.get("reason");
                if (studentId != null && extendedEndTimeStr != null) {
                    Date extendedEndTime = sdf.parse(extendedEndTimeStr);
                    deadlineExtensionMapper.upsert(assignmentName, studentId, extendedEndTime, reason);
                    count++;
                }
            }
            return Result.success("批量延期设置成功，共处理 " + count + " 名学生");
        } catch (java.text.ParseException e) {
            return Result.error("日期格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        }
    }

    /** 撤销某学生的延期 */
    @DeleteMapping("/{assignmentName}/extend/{studentId}")
    public Result<String> revokeExtension(@PathVariable String assignmentName,
                                          @PathVariable String studentId) {
        deadlineExtensionMapper.delete(assignmentName, studentId);
        return Result.success("延期已撤销");
    }

    // ──────────────────────────────────────────────
    // 相似度查询
    // ──────────────────────────────────────────────

    /** 查询某作业的所有提交相似度结果 */
    @GetMapping("/{assignmentName}/similarity")
    public Result<List<AssignmentSimilarity>> getSimilarities(
            @PathVariable String assignmentName,
            @RequestParam(required = false) String studentId) {
        if (studentId != null && !studentId.isBlank()) {
            return Result.success(similarityService.getSimilaritiesForStudent(assignmentName, studentId));
        }
        return Result.success(similarityService.getSimilarities(assignmentName));
    }

    /** 触发相似度计算（异步，结果逐条入库） */
    @PostMapping("/{assignmentName}/similarity/compute")
    public Result<java.util.Map<String, Object>> computeSimilarities(@PathVariable String assignmentName) {
        int total = similarityService.getTotalPairs(assignmentName);
        if (total == 0) {
            return Result.error("提交数量不足（至少需要2份提交），无法计算相似度");
        }
        similarityService.computeSimilaritiesAsync(assignmentName);
        return Result.success(java.util.Map.of("totalPairs", total, "message", "开始计算，共 " + total + " 对"));
    }
}
