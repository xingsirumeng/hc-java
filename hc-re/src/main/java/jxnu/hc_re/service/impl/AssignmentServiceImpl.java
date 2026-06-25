package jxnu.hc_re.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jxnu.hc_re.mapper.AssignmentMapper;
import jxnu.hc_re.mapper.SubmissionMapper;
import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.StudentAssignmentDTO;
import jxnu.hc_re.pojo.Submission;
import jxnu.hc_re.service.AssignmentService;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Override
    public List<Assignment> getAssignmentsByCourse(String courseId, String teacherId) {
        return assignmentMapper.findByCourseIdAndTeacher(courseId, teacherId);
    }

    @Override
    public List<Assignment> findAll() {
        return assignmentMapper.findAll();
    }

    @Override
    public void createAssignment(Assignment assignment) {
        assignmentMapper.insert(assignment);
    }

    @Override
    public void updateAssignment(Assignment assignment) {
        assignmentMapper.update(assignment);
    }

    @Override
    public void deleteAssignment(String name) {
        assignmentMapper.deleteByName(name);
    }

    @Override
    public Assignment findByName(String name) {
        return assignmentMapper.findByName(name);
    }

    @Override
    public List<Submission> getSubmissions(String assignmentName) {
        return submissionMapper.findByAssignmentName(assignmentName);
    }

    @Override
    public List<StudentAssignmentDTO> getStudentAssignments(String studentId) {
        return assignmentMapper.findByStudentId(studentId);
    }

    @Override
    public StudentAssignmentDTO getStudentAssignmentDetail(String assignmentName, String studentId) {
        return assignmentMapper.findOneByStudentId(assignmentName, studentId);
    }
}
