package jxnu.hc_re.service.impl;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jxnu.hc_re.mapper.SubmissionMapper;
import jxnu.hc_re.service.AiService;
import jxnu.hc_re.service.FileService;

@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private FileService fileService;

    private final RestTemplate restTemplate;

    private static final String AI_SERVICE = "http://localhost:8000";

    public AiServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> grade(String filepath, String requirement) {
        File file = fileService.resolveFile(filepath);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("assignment", requirement);
        form.add("file", new FileSystemResource(file));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                AI_SERVICE + "/api/v1/grade/upload", request, Map.class);
        Map<String, Object> result = response.getBody();
        if (result == null || result.containsKey("error")) {
            throw new RuntimeException(result != null ? String.valueOf(result.get("error")) : "AI 服务返回为空");
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> aigcDetect(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> reqBody = Map.of("text", text);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(reqBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                AI_SERVICE + "/api/v1/aigc/detect", request, Map.class);
        Map<String, Object> result = response.getBody();
        if (result == null || result.containsKey("error")) {
            throw new RuntimeException(result != null ? String.valueOf(result.get("error")) : "AIGC 服务返回为空");
        }
        return stripAigcResult(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> aigcDetectFile(String filepath, String assignmentName, String studentId) {
        File file = fileService.resolveFile(filepath);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new FileSystemResource(file));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                AI_SERVICE + "/api/v1/aigc/detect/upload", request, Map.class);
        Map<String, Object> result = response.getBody();
        if (result == null || result.containsKey("error")) {
            throw new RuntimeException(result != null ? String.valueOf(result.get("error")) : "AIGC 服务返回为空");
        }

        Map<String, Object> stripped = stripAigcResult(result);
        int score = ((Number) stripped.get("aigc_score")).intValue();
        String label = (String) stripped.get("label");

        if (assignmentName != null && studentId != null) {
            submissionMapper.updateAigc(assignmentName, studentId, score);
        }

        return stripped;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> processFile(String filepath) {
        File file = fileService.resolveFile(filepath);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new FileSystemResource(file));
        form.add("with_location", "false");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                AI_SERVICE + "/api/v1/process/upload", request, Map.class);
        Map<String, Object> result = response.getBody();
        if (result == null) {
            throw new RuntimeException("AI 文本提取服务返回为空");
        }
        // Python ProcessResponse 始终带 error 字段（默认空串），只有非空时才算真错误
        Object errorObj = result.get("error");
        if (errorObj instanceof String && !((String) errorObj).isEmpty()) {
            throw new RuntimeException((String) errorObj);
        }
        return result;
    }

    // ── 工具方法 ──

    @SuppressWarnings("unchecked")
    private Map<String, Object> stripAigcResult(Map<String, Object> result) {
        Map<String, Object> aigcResult = (Map<String, Object>) result.get("aigc_result");
        if (aigcResult != null) {
            return Map.of(
                "aigc_score", aigcResult.getOrDefault("aigc_score", 0),
                "suggestion", aigcResult.getOrDefault("suggestion", ""),
                "label", aigcResult.getOrDefault("label", "")
            );
        }
        return Map.of(
            "aigc_score", result.getOrDefault("aigc_score", 0),
            "suggestion", result.getOrDefault("suggestion", ""),
            "label", result.getOrDefault("label", "")
        );
    }
}
