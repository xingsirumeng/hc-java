package jxnu.hc_re.service;

import java.util.List;

import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.StudentAssignmentDTO;
import jxnu.hc_re.pojo.Submission;

public interface AssignmentService {
    List<Assignment> getAssignmentsByCourse(String courseId, String teacherId);
    List<Assignment> findAll();
    void createAssignment(Assignment assignment);
    void updateAssignment(Assignment assignment);
    void deleteAssignment(String name);
    Assignment findByName(String name);
    List<Submission> getSubmissions(String assignmentName);

    /** 学生视角：获取已选课程的所有作业（带提交状态） */
    List<StudentAssignmentDTO> getStudentAssignments(String studentId);

    /** 学生视角：单个作业详情 */
    StudentAssignmentDTO getStudentAssignmentDetail(String assignmentName, String studentId);
}
