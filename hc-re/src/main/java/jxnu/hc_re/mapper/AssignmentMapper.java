package jxnu.hc_re.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;

import jxnu.hc_re.pojo.Assignment;
import jxnu.hc_re.pojo.StudentAssignmentDTO;

@Mapper
public interface AssignmentMapper {

    @Select("SELECT * FROM assignment WHERE course_id = #{courseId}")
    List<Assignment> findByCourseId(String courseId);

    @Select("SELECT * FROM assignment WHERE name = #{name}")
    Assignment findByName(String name);

    @Select("SELECT * FROM assignment WHERE course_id = #{courseId} AND course_id IN " +
            "(SELECT course_id FROM courses WHERE teacher_id = #{teacherId})")
    List<Assignment> findByCourseIdAndTeacher(String courseId, String teacherId);

    @Insert("INSERT INTO assignment (name, start_time, end_time, course_id, requirement) " +
            "VALUES (#{name}, #{startTime}, #{endTime}, #{courseId}, #{requirement})")
    void insert(Assignment assignment);

    @Delete("DELETE FROM assignment WHERE name = #{name}")
    void deleteByName(String name);

    @Update("UPDATE assignment SET start_time = #{startTime}, end_time = #{endTime}, " +
            "course_id = #{courseId}, requirement = #{requirement} WHERE name = #{name}")
    void update(Assignment assignment);

    @Select("SELECT * FROM assignment")
    List<Assignment> findAll();

    /** 学生视角：获取已选课程的所有作业，带提交状态和成绩 */
    @Select("SELECT a.name AS assignmentName, a.start_time AS startTime, a.end_time AS endTime, " +
            "a.course_id AS courseId, a.requirement, " +
            "c.course_name AS courseName, " +
            "s.submission_time AS submissionTime, s.filepath, s.original_filename AS originalFilename, " +
            "s.score, s.comment, s.aigc_score AS aigcScore, " +
            "s.annotated_filepath AS annotatedFilepath, " +
            "s.status, s.return_reason AS returnReason, " +
            "s.grade_attachments AS gradeAttachments, " +
            "CASE WHEN s.student_id IS NOT NULL THEN true ELSE false END AS submitted " +
            "FROM assignment a " +
            "JOIN courses c ON a.course_id = c.course_id " +
            "JOIN sc ON a.course_id = sc.course_id AND sc.student_id = #{studentId} " +
            "LEFT JOIN submission s ON a.name = s.assignment_name AND s.student_id = #{studentId} " +
            "ORDER BY a.end_time ASC")
    @Results({
        @Result(property = "assignmentName", column = "assignmentName"),
        @Result(property = "submitted", column = "submitted")
    })
    List<StudentAssignmentDTO> findByStudentId(String studentId);

    /** 学生视角：单个作业详情（带提交状态） */
    @Select("SELECT a.name AS assignmentName, a.start_time AS startTime, a.end_time AS endTime, " +
            "a.course_id AS courseId, a.requirement, " +
            "c.course_name AS courseName, " +
            "s.submission_time AS submissionTime, s.filepath, s.original_filename AS originalFilename, " +
            "s.score, s.comment, s.aigc_score AS aigcScore, " +
            "s.annotated_filepath AS annotatedFilepath, " +
            "s.status, s.return_reason AS returnReason, " +
            "s.grade_attachments AS gradeAttachments, " +
            "CASE WHEN s.student_id IS NOT NULL THEN true ELSE false END AS submitted " +
            "FROM assignment a " +
            "JOIN courses c ON a.course_id = c.course_id " +
            "JOIN sc ON a.course_id = sc.course_id AND sc.student_id = #{studentId} " +
            "LEFT JOIN submission s ON a.name = s.assignment_name AND s.student_id = #{studentId} " +
            "WHERE a.name = #{assignmentName}")
    @Results({
        @Result(property = "assignmentName", column = "assignmentName"),
        @Result(property = "submitted", column = "submitted")
    })
    StudentAssignmentDTO findOneByStudentId(String assignmentName, String studentId);
}
