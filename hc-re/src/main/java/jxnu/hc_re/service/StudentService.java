package jxnu.hc_re.service;

import java.util.List;

import jxnu.hc_re.pojo.Student;

public interface StudentService {
    List<Student> getStudentsByCourseId(String courseId);
    List<Student> findAll();
    Student findById(String studentId);
    void updateProfile(String studentId, String name, String newPassword);
    void addStudent(Student student);
    void updateStudent(Student student);
    void deleteStudent(String studentId);
}
