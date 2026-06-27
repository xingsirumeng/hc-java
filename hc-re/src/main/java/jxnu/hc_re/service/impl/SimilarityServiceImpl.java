package jxnu.hc_re.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                        detail = String.format("%s（%d字） vs %s（%d字） — %.2f%%",
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

    private BigDecimal computeJaccard(String t1, String t2) {
        t1 = t1.replaceAll("\\s+", " ").trim();
        t2 = t2.replaceAll("\\s+", " ").trim();

        if (t1.isEmpty() && t2.isEmpty()) return BigDecimal.valueOf(100);
        if (t1.isEmpty() || t2.isEmpty()) return BigDecimal.ZERO;

        int n = isCJK(t1) || isCJK(t2) ? 1 : 3;
        Set<String> a = ngrams(t1, n), b = ngrams(t2, n);
        Set<String> inter = new HashSet<>(a); inter.retainAll(b);
        Set<String> union = new HashSet<>(a); union.addAll(b);

        if (union.isEmpty()) return BigDecimal.valueOf(100);
        return BigDecimal.valueOf(100.0 * inter.size() / union.size()).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isCJK(String s) {
        int cjk = 0;
        for (char c : s.toCharArray()) {
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) cjk++;
        }
        return cjk > s.replaceAll("[^a-zA-Z]", "").length();
    }

    private Set<String> ngrams(String s, int n) {
        Set<String> set = new HashSet<>();
        for (int i = 0; i <= s.length() - n; i++) set.add(s.substring(i, i + n));
        return set;
    }
}
