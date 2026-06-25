package jxnu.hc_re.service;

import java.util.List;

import jxnu.hc_re.pojo.Sc;

public interface ScService {
    List<Sc> findAll();
    List<Sc> findByCourseId(String courseId);
    List<Sc> findByStudentId(String studentId);
    void enroll(String studentId, String courseId);
    void unenroll(Integer scId);
    void unenrollByStudentAndCourse(String studentId, String courseId);
}
