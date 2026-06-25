package jxnu.hc_re.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.service.AiService;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /** AI 评阅 */
    @PostMapping("/grade")
    public Result<Map<String, Object>> aiGrade(@RequestBody Map<String, Object> body) {
        String filepath = (String) body.get("filepath");
        String requirement = (String) body.get("requirement");
        if (filepath == null || requirement == null || requirement.isBlank()) {
            return Result.error("缺少文件路径或作业要求");
        }
        try {
            return Result.success(aiService.grade(filepath, requirement));
        } catch (Exception e) {
            return Result.error("AI 服务调用失败: " + e.getMessage());
        }
    }

    /** AIGC 文本检测 */
    @PostMapping("/aigc-detect")
    public Result<Map<String, Object>> aigcDetect(@RequestBody Map<String, Object> body) {
        String text = (String) body.get("text");
        if (text == null || text.isBlank()) {
            return Result.error("缺少待检测文本");
        }
        try {
            return Result.success(aiService.aigcDetect(text));
        } catch (Exception e) {
            return Result.error("AIGC 服务调用失败: " + e.getMessage());
        }
    }

    /** AIGC 文件检测 */
    @PostMapping("/aigc-detect-file")
    public Result<Map<String, Object>> aigcDetectFile(@RequestBody Map<String, Object> body) {
        String filepath = (String) body.get("filepath");
        if (filepath == null) {
            return Result.error("缺少文件路径");
        }
        try {
            String assignmentName = (String) body.get("assignmentName");
            String studentId = (String) body.get("studentId");
            return Result.success(aiService.aigcDetectFile(filepath, assignmentName, studentId));
        } catch (Exception e) {
            return Result.error("AIGC 服务调用失败: " + e.getMessage());
        }
    }
}
