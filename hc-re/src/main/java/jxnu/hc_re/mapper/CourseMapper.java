package jxnu.hc_re.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import jxnu.hc_re.pojo.Course;

@Mapper
public interface CourseMapper {
    @Select("SELECT * FROM courses")
    List<Course> findAllCourses();

    @Select("SELECT * FROM courses WHERE course_id = #{courseId}")
    Course findById(String courseId);

    /** 查询某位教师所教的所有课程 */
    @Select("SELECT * FROM courses WHERE teacher_id = #{teacherId}")
    List<Course> findByTeacherId(String teacherId);

    /** 查询某位学生选修的所有课程 */
    @Select("SELECT c.* FROM courses c JOIN sc ON c.course_id = sc.course_id WHERE sc.student_id = #{studentId}")
    List<Course> findByStudentId(String studentId);

    @Insert("INSERT INTO courses (course_id, course_name, teacher_id, credit) VALUES (#{courseId}, #{courseName}, #{teacherId}, #{credit})")
    void addCourse(Course course);

    @Update("UPDATE courses SET course_name = #{courseName}, teacher_id = #{teacherId}, credit = #{credit} WHERE course_id = #{courseId}")
    void updateCourse(Course course);

    @Delete("DELETE FROM courses WHERE course_id = #{courseId}")
    void deleteCourse(String courseId);
}
