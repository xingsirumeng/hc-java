package jxnu.hc_re.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import jxnu.hc_re.pojo.Submission;

@Mapper
public interface SubmissionMapper {

    @Select("SELECT * FROM submission WHERE assignment_name = #{assignmentName}")
    List<Submission> findByAssignmentName(String assignmentName);

    @Select("SELECT * FROM submission WHERE student_id = #{studentId} ORDER BY submission_time DESC")
    List<Submission> findByStudentId(String studentId);

    @Select("SELECT * FROM submission WHERE assignment_name = #{assignmentName} AND student_id = #{studentId}")
    Submission findByStudentAndAssignment(String assignmentName, String studentId);

    @Update("UPDATE submission SET score = #{score}, comment = #{comment}, " +
            "annotated_filepath = #{annotatedFilepath} " +
            "WHERE assignment_name = #{assignmentName} AND student_id = #{studentId}")
    void updateGrade(String assignmentName, String studentId, Integer score,
                     String comment, String annotatedFilepath);

    @Insert("INSERT INTO submission (assignment_name, student_id, submission_time, filepath, original_filename) " +
            "VALUES (#{assignmentName}, #{studentId}, #{submissionTime}, #{filepath}, #{originalFilename}) " +
            "ON CONFLICT (assignment_name, student_id) DO UPDATE SET " +
            "submission_time = EXCLUDED.submission_time, " +
            "filepath = EXCLUDED.filepath, " +
            "original_filename = EXCLUDED.original_filename, " +
            "score = NULL, comment = NULL, aigc_score = NULL")
    void insert(Submission submission);

    @Update("UPDATE submission SET aigc_score = #{aigcScore} " +
            "WHERE assignment_name = #{assignmentName} AND student_id = #{studentId}")
    void updateAigc(String assignmentName, String studentId, Integer aigcScore);

    @Select("SELECT * FROM submission ORDER BY submission_time DESC")
    List<Submission> findAll();

    @Delete("DELETE FROM submission WHERE assignment_name = #{assignmentName} AND student_id = #{studentId}")
    void deleteByAssignmentAndStudent(String assignmentName, String studentId);
}
