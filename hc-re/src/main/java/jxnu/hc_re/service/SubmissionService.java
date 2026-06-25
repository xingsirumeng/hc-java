package jxnu.hc_re.service;

import java.util.List;

import jxnu.hc_re.pojo.Submission;

public interface SubmissionService {
    void grade(String assignmentName, String studentId, Integer score, String comment, String annotatedFilepath);

    /** 学生提交作业（含截止时间和选课校验） */
    void submit(String assignmentName, String studentId, String filepath, String originalFilename);

    /** 学生查看自己的所有提交 */
    List<Submission> getByStudentId(String studentId);

    /** 学生查看某个作业的提交详情 */
    Submission getByStudentAndAssignment(String assignmentName, String studentId);

    /** 管理员查看所有提交 */
    List<Submission> findAll();

    /** 管理员删除提交记录 */
    void deleteSubmission(String assignmentName, String studentId);
}
