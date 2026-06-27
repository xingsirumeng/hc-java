package jxnu.hc_re.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import jxnu.hc_re.pojo.AssignmentSimilarity;

@Mapper
public interface AssignmentSimilarityMapper {

    @Select("SELECT * FROM assignment_similarity WHERE assignment_name = #{assignmentName} ORDER BY similarity_score DESC")
    List<AssignmentSimilarity> findByAssignmentName(String assignmentName);

    @Select("SELECT * FROM assignment_similarity WHERE assignment_name = #{assignmentName} " +
            "AND ((student_id_1 = #{studentId}) OR (student_id_2 = #{studentId})) ORDER BY similarity_score DESC")
    List<AssignmentSimilarity> findByAssignmentAndStudent(String assignmentName, String studentId);

    @Insert("INSERT INTO assignment_similarity (assignment_name, student_id_1, student_id_2, similarity_score, detail) " +
            "VALUES (#{assignmentName}, #{studentId1}, #{studentId2}, #{similarityScore}, #{detail}) " +
            "ON CONFLICT (assignment_name, student_id_1, student_id_2) DO UPDATE SET " +
            "similarity_score = EXCLUDED.similarity_score, detail = EXCLUDED.detail")
    void upsert(String assignmentName, String studentId1, String studentId2,
                BigDecimal similarityScore, String detail);

    @Delete("DELETE FROM assignment_similarity WHERE assignment_name = #{assignmentName}")
    void deleteByAssignmentName(String assignmentName);
}
