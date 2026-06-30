package jxnu.hc_re.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jxnu.hc_re.mapper.AssignmentSimilarityMapper;
import jxnu.hc_re.mapper.SubmissionMapper;
import jxnu.hc_re.pojo.AssignmentSimilarity;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.service.AiService;
import jxnu.hc_re.service.SimilarityService;

@Service
public class SimilarityServiceImpl implements SimilarityService {

    private static final Logger log = LoggerFactory.getLogger(SimilarityServiceImpl.class);

    @Autowired
    private AssignmentSimilarityMapper similarityMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private AiService aiService;

    @Override
    public List<AssignmentSimilarity> getSimilarities(String assignmentName) {
        return similarityMapper.findByAssignmentName(assignmentName);
    }

    @Override
    public List<AssignmentSimilarity> getSimilaritiesForStudent(String assignmentName, String studentId) {
        return similarityMapper.findByAssignmentAndStudent(assignmentName, studentId);
    }

    @Override
    public int getTotalPairs(String assignmentName) {
        List<Submission> submissions = submissionMapper.findByAssignmentName(assignmentName);
        int n = submissions != null ? submissions.size() : 0;
        return n * (n - 1) / 2;
    }

    @Override
    @Transactional
    public void computeSimilaritiesAsync(String assignmentName) {
        // 清空旧结果
        similarityMapper.deleteByAssignmentName(assignmentName);

        List<Submission> submissions = submissionMapper.findByAssignmentName(assignmentName);
        if (submissions == null || submissions.size() < 2) {
            return;
        }

        for (int i = 0; i < submissions.size(); i++) {
            for (int j = i + 1; j < submissions.size(); j++) {
                Submission s1 = submissions.get(i);
                Submission s2 = submissions.get(j);

                try {
                    String text1 = extractText(s1);
                    String text2 = extractText(s2);

                    BigDecimal score;
                    String detail;

                    if (text1.isEmpty() && text2.isEmpty()) {
                        score = BigDecimal.valueOf(-1);
                        detail = "无法计算：文本提取均失败，请确认 Python AI 服务已启动";
                    } else if (text1.isEmpty() || text2.isEmpty()) {
                        score = BigDecimal.valueOf(-1);
                        detail = String.format("无法计算：%s 文本提取失败",
                            text1.isEmpty() ? s1.getStudentId() : s2.getStudentId());
                    } else {
                        score = computeJaccard(text1, text2);
                        detail = String.format("%s（%d字） vs %s（%d字） — TF余弦 %.2f%%",
                            s1.getStudentId(), text1.length(),
                            s2.getStudentId(), text2.length(),
                            score.doubleValue());
                    }

                    similarityMapper.upsert(assignmentName, s1.getStudentId(), s2.getStudentId(), score, detail);
                    log.info("相似度 {} vs {} = {}", s1.getStudentId(), s2.getStudentId(), score);
                } catch (Exception e) {
                    log.warn("相似度异常 {} vs {}: {}", s1.getStudentId(), s2.getStudentId(), e.toString());
                    similarityMapper.upsert(assignmentName, s1.getStudentId(), s2.getStudentId(),
                        BigDecimal.valueOf(-1), "异常: " + e.getMessage());
                }
            }
        }
        log.info("相似度完成: {}", assignmentName);
    }

    private String extractText(Submission sub) {
        try {
            Map<String, Object> result = aiService.processFile(sub.getFilepath());
            if (Boolean.TRUE.equals(result.get("success"))) {
                Object text = result.get("text");
                return text != null ? text.toString() : "";
            }
            log.warn("提取失败 {}: {}", sub.getStudentId(), result.get("error"));
            return "";
        } catch (Exception e) {
            log.warn("提取异常 {}: {}", sub.getStudentId(), e.toString());
            return "";
        }
    }

    /**
     * TF-加权余弦相似度。
     * 中文用 2-gram 分词，英文用空格分词 + 小写。
     */
    private BigDecimal computeJaccard(String t1, String t2) {
        t1 = t1.trim();
        t2 = t2.trim();
        if (t1.isEmpty() && t2.isEmpty()) return BigDecimal.valueOf(100);
        if (t1.isEmpty() || t2.isEmpty()) return BigDecimal.ZERO;

        List<String> tokens1 = tokenize(t1);
        List<String> tokens2 = tokenize(t2);
        if (tokens1.isEmpty() && tokens2.isEmpty()) return BigDecimal.valueOf(100);
        if (tokens1.isEmpty() || tokens2.isEmpty()) return BigDecimal.ZERO;

        // 构建词频
        Map<String, Integer> freq1 = new HashMap<>();
        Map<String, Integer> freq2 = new HashMap<>();
        for (String w : tokens1) freq1.merge(w, 1, Integer::sum);
        for (String w : tokens2) freq2.merge(w, 1, Integer::sum);

        // 收集所有词
        Set<String> allWords = new HashSet<>(freq1.keySet());
        allWords.addAll(freq2.keySet());

        // 余弦 = dot / (|v1| * |v2|)
        double dot = 0, norm1 = 0, norm2 = 0;
        for (String w : allWords) {
            int f1 = freq1.getOrDefault(w, 0);
            int f2 = freq2.getOrDefault(w, 0);
            // TF-IDF 简化：用对数频率
            double tf1 = f1 > 0 ? 1 + Math.log(f1) : 0;
            double tf2 = f2 > 0 ? 1 + Math.log(f2) : 0;
            dot += tf1 * tf2;
            norm1 += tf1 * tf1;
            norm2 += tf2 * tf2;
        }

        if (norm1 == 0 || norm2 == 0) return BigDecimal.ZERO;
        double cos = dot / (Math.sqrt(norm1) * Math.sqrt(norm2));

        // 对极低相似度做放大，让差异更明显
        double score = Math.round(cos * 10000.0) / 100.0;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /** 分词：中文 2-gram，英文按空格+标点切分 */
    private List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        boolean isCJK = false;
        for (char c : s.toCharArray()) {
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) { isCJK = true; break; }
        }

        if (isCJK) {
            // 中文：2-gram
            String cleaned = s.replaceAll("[^\\u4E00-\\u9FFF\\u3400-\\u4DBF]", "");
            for (int i = 0; i <= cleaned.length() - 2; i++) {
                tokens.add(cleaned.substring(i, i + 2));
            }
        } else {
            // 英文/混合：小写 + 按非字母数字分割
            String[] words = s.toLowerCase().split("[^a-z0-9]+");
            for (String w : words) {
                if (w.length() >= 2) tokens.add(w);
            }
        }
        return tokens;
    }
}
