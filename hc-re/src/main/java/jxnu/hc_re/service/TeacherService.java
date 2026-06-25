package jxnu.hc_re.service;

import jxnu.hc_re.pojo.Teacher;
import java.util.List;

public interface TeacherService {
    List<Teacher> findAllTeachers();
    void addTeacher(Teacher teacher);
    Teacher findById(String teacherId);
    void updateTeacher(Teacher teacher);
    void deleteTeacher(String teacherId);
    void updateProfile(String teacherId, String name, String password);
}
