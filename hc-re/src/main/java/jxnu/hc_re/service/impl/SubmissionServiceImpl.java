package jxnu.hc_re.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jxnu.hc_re.mapper.AssignmentMapper;
import jxnu.hc_re.mapper.SubmissionMapper;
import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.service.SubmissionService;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Override
    public void grade(String assignmentName, String studentId, Integer score, String comment, String annotatedFilepath) {
        submissionMapper.updateGrade(assignmentName, studentId, score, comment, annotatedFilepath);
    }

    @Override
    public void submit(String assignmentName, String studentId, String filepath, String originalFilename) {
        Assignment assignment = assignmentMapper.findByName(assignmentName);
        if (assignment == null) {
            throw new RuntimeException("作业不存在: " + assignmentName);
        }

        // 检查是否超过截止时间
        Date now = new Date();
        if (now.after(assignment.getEndTime())) {
            throw new RuntimeException("已超过作业截止时间，无法提交");
        }

        Submission submission = new Submission();
        submission.setAssignmentName(assignmentName);
        submission.setStudentId(studentId);
        submission.setSubmissionTime(now);
        submission.setFilepath(filepath);
        submission.setOriginalFilename(originalFilename);

        // insert 自带 UPSERT（ON CONFLICT），重复提交会覆盖之前的文件并重置评分
        submissionMapper.insert(submission);
    }

    @Override
    public List<Submission> getByStudentId(String studentId) {
        return submissionMapper.findByStudentId(studentId);
    }

    @Override
    public Submission getByStudentAndAssignment(String assignmentName, String studentId) {
        return submissionMapper.findByStudentAndAssignment(assignmentName, studentId);
    }

    @Override
    public List<Submission> findAll() {
        return submissionMapper.findAll();
    }

    @Override
    public void deleteSubmission(String assignmentName, String studentId) {
        submissionMapper.deleteByAssignmentAndStudent(assignmentName, studentId);
    }
}
