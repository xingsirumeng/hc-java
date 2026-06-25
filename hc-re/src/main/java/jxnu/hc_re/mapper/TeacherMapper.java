package jxnu.hc_re.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import jxnu.hc_re.pojo.Teacher;

@Mapper
public interface TeacherMapper {  //mybatis的mapper接口(会自己生成实现类)
    @Insert("INSERT INTO teacher (teacher_id, name, password) VALUES (#{teacherId}, #{name}, #{password})")
    void addTeacher(Teacher teacher);
    @Select("SELECT * FROM teacher")
    List<Teacher> findAllTeachers();

    @Select("SELECT * FROM teacher WHERE teacher_id = #{teacherId}")
    Teacher findById(String teacherId);

    @Update("UPDATE teacher SET name = #{name}, password = #{password} WHERE teacher_id = #{teacherId}")
    void updateTeacher(Teacher teacher);

    @Delete("DELETE FROM teacher WHERE teacher_id = #{teacherId}")
    void deleteTeacher(String teacherId);

    @Update("UPDATE teacher SET password = #{password} WHERE teacher_id = #{teacherId}")
    void updateTeacherPassword(String teacherId, String password);

    /** 更新教师资料（name 为 null 时不更新 name，password 为 null 时不更新 password） */
    @Update("<script>" +
            "UPDATE teacher " +
            "<set>" +
            "<if test='name != null'>name = #{name},</if>" +
            "<if test='password != null'>password = #{password},</if>" +
            "</set>" +
            "WHERE teacher_id = #{teacherId}" +
            "</script>")
    void updateProfile(String teacherId, String name, String password);
}
