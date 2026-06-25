package jxnu.hc_re.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import jxnu.hc_re.pojo.Sc;

@Mapper
public interface ScMapper {

    @Select("SELECT * FROM sc")
    List<Sc> findAll();

    @Select("SELECT * FROM sc WHERE sc_id = #{scId}")
    Sc findById(Integer scId);

    @Select("SELECT * FROM sc WHERE course_id = #{courseId}")
    List<Sc> findByCourseId(String courseId);

    @Select("SELECT * FROM sc WHERE student_id = #{studentId}")
    List<Sc> findByStudentId(String studentId);

    @Insert("INSERT INTO sc (student_id, course_id) VALUES (#{studentId}, #{courseId})")
    @Options(useGeneratedKeys = true, keyProperty = "scId")
    void insert(Sc sc);

    @Delete("DELETE FROM sc WHERE sc_id = #{scId}")
    void deleteById(Integer scId);

    @Delete("DELETE FROM sc WHERE student_id = #{studentId} AND course_id = #{courseId}")
    void deleteByStudentAndCourse(String studentId, String courseId);
}
