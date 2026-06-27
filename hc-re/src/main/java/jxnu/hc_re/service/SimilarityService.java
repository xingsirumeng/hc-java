package jxnu.hc_re.service;

import java.util.List;

import jxnu.hc_re.pojo.AssignmentSimilarity;

public interface SimilarityService {
    /** 查询某作业的相似度结果 */
    List<AssignmentSimilarity> getSimilarities(String assignmentName);

    /** 查询某作业中与某学生相关的相似度 */
    List<AssignmentSimilarity> getSimilaritiesForStudent(String assignmentName, String studentId);

    /** 异步计算某作业所有提交之间的相似度（立即返回，结果逐条入库） */
    void computeSimilaritiesAsync(String assignmentName);

    /** 获取计算进度：total pair count */
    int getTotalPairs(String assignmentName);
}
