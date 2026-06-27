package jxnu.hc_re.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jxnu.hc_re.mapper.AssignmentMapper;
import jxnu.hc_re.mapper.DeadlineExtensionMapper;
import jxnu.hc_re.mapper.SubmissionMapper;
import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.DeadlineExtension;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.service.SubmissionService;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private DeadlineExtensionMapper deadlineExtensionMapper;

    @Override
    public void grade(String assignmentName, String studentId, Integer score, String comment, String annotatedFilepath, String gradeAttachments) {
        submissionMapper.updateGrade(assignmentName, studentId, score, comment, annotatedFilepath, gradeAttachments);
    }

    @Override
    @Transactional
    public void batchGrade(List<Map<String, Object>> gradeList) {
        for (Map<String, Object> item : gradeList) {
            String assignmentName = (String) item.get("assignmentName");
            String studentId = (String) item.get("studentId");
            Integer score = item.get("score") != null ? ((Number) item.get("score")).intValue() : null;
            String comment = (String) item.get("comment");
            String annotatedFilepath = (String) item.get("annotatedFilepath");
            String gradeAttachments = (String) item.get("gradeAttachments");
            submissionMapper.updateGrade(assignmentName, studentId, score, comment, annotatedFilepath, gradeAttachments);
        }
    }

    @Override
    public void submit(String assignmentName, String studentId, String filepath, String originalFilename) {
        Assignment assignment = assignmentMapper.findByName(assignmentName);
        if (assignment == null) {
            throw new RuntimeException("作业不存在: " + assignmentName);
        }

        // 检查是否已打回 — 打回的作业允许重新提交（不受截止时间限制）
        Submission existing = submissionMapper.findByStudentAndAssignment(assignmentName, studentId);
        boolean isResubmit = existing != null && "returned".equals(existing.getStatus());

        // 非打回重交时检查截止时间（优先检查延期）
        if (!isResubmit) {
            Date now = new Date();
            Date effectiveEndTime = assignment.getEndTime();

            // 检查是否有延期记录
            DeadlineExtension extension = deadlineExtensionMapper.findByAssignmentAndStudent(assignmentName, studentId);
            if (extension != null && extension.getExtendedEndTime() != null) {
                effectiveEndTime = extension.getExtendedEndTime();
            }

            if (now.after(effectiveEndTime)) {
                throw new RuntimeException("已超过作业截止时间，无法提交");
            }
        }

        Submission submission = new Submission();
        submission.setAssignmentName(assignmentName);
        submission.setStudentId(studentId);
        submission.setSubmissionTime(new Date());
        submission.setFilepath(filepath);
        submission.setOriginalFilename(originalFilename);

        // insert 自带 UPSERT（ON CONFLICT），重复提交会覆盖之前的文件并重置评分
        submissionMapper.insert(submission);
    }

    @Override
    public void returnSubmission(String assignmentName, String studentId, String reason) {
        Submission submission = submissionMapper.findByStudentAndAssignment(assignmentName, studentId);
        if (submission == null) {
            throw new RuntimeException("该学生未提交此作业");
        }
        submissionMapper.updateStatus(assignmentName, studentId, "returned", reason);
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
