package jxnu.hc_re.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import jxnu.hc_re.pojo.DeadlineExtension;

@Mapper
public interface DeadlineExtensionMapper {

    @Select("SELECT * FROM deadline_extension WHERE assignment_name = #{assignmentName}")
    List<DeadlineExtension> findByAssignmentName(String assignmentName);

    @Select("SELECT * FROM deadline_extension WHERE assignment_name = #{assignmentName} AND student_id = #{studentId}")
    DeadlineExtension findByAssignmentAndStudent(String assignmentName, String studentId);

    @Insert("INSERT INTO deadline_extension (assignment_name, student_id, extended_end_time, reason) " +
            "VALUES (#{assignmentName}, #{studentId}, #{extendedEndTime}, #{reason}) " +
            "ON CONFLICT (assignment_name, student_id) DO UPDATE SET " +
            "extended_end_time = EXCLUDED.extended_end_time, " +
            "reason = EXCLUDED.reason")
    void upsert(String assignmentName, String studentId, Date extendedEndTime, String reason);

    @Delete("DELETE FROM deadline_extension WHERE assignment_name = #{assignmentName} AND student_id = #{studentId}")
    void delete(String assignmentName, String studentId);

    @Select("SELECT * FROM deadline_extension WHERE student_id = #{studentId}")
    List<DeadlineExtension> findByStudentId(String studentId);
}
