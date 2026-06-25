package jxnu.hc_re.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import jxnu.hc_re.pojo.Student;

@Mapper
public interface StudentMapper {
    @Select("SELECT * FROM student")
    List<Student> findAllStudents();

    @Select("SELECT * FROM student WHERE student_id = #{studentId}")
    Student findById(String studentId);

    @Insert("INSERT INTO student (student_id, name, password) VALUES (#{studentId}, #{name}, #{password})")
    void addStudent(Student student);

    @Update("UPDATE student SET name = #{name}, password = #{password} WHERE student_id = #{studentId}")
    void updateStudent(Student student);

    /** 更新学生资料（name 为 null 时不更新 name，password 为 null 时不更新 password） */
    @Update("<script>" +
            "UPDATE student " +
            "<set>" +
            "<if test='name != null'>name = #{name},</if>" +
            "<if test='password != null'>password = #{password},</if>" +
            "</set>" +
            "WHERE student_id = #{studentId}" +
            "</script>")
    void updateProfile(String studentId, String name, String password);

    @Delete("DELETE FROM student WHERE student_id = #{studentId}")
    void deleteStudent(String studentId);

    @Update("UPDATE student SET password = #{password} WHERE student_id = #{studentId}")
    void updateStudentPassword(String studentId, String password);

    /** 查询某课程的学生（通过 sc 关联） */
    @Select("SELECT s.* FROM student s " +
            "JOIN sc ON s.student_id = sc.student_id " +
            "WHERE sc.course_id = #{courseId}")
    List<Student> findByCourseId(String courseId);
}
