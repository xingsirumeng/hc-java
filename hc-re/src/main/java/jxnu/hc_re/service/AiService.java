package jxnu.hc_re.service;

import java.util.Map;

public interface AiService {
    /** AI 评阅：发送学生文件到 AI 服务获取评分和评语 */
    Map<String, Object> grade(String filepath, String requirement);

    /** AIGC 文本检测 */
    Map<String, Object> aigcDetect(String text);

    /** AIGC 文件检测：上传文件 → 提取文本 → 检测 → 结果存库 */
    Map<String, Object> aigcDetectFile(String filepath, String assignmentName, String studentId);
}
