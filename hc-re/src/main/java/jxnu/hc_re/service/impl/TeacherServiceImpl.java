package jxnu.hc_re.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import jxnu.hc_re.mapper.TeacherMapper;
import jxnu.hc_re.pojo.Teacher;
import jxnu.hc_re.service.TeacherService;
import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {

    @Autowired
    private TeacherMapper teacherMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public List<Teacher> findAllTeachers() {
        return teacherMapper.findAllTeachers();
    }

    @Override
    public void addTeacher(Teacher teacher) {
        // 密码加密后存储
        teacher.setPassword(encoder.encode(teacher.getPassword()));
        teacherMapper.addTeacher(teacher);
    }

    @Override
    public Teacher findById(String teacherId) {
        return teacherMapper.findById(teacherId);
    }

    @Override
    public void updateTeacher(Teacher teacher) {
        // 更新时也加密密码
        if (teacher.getPassword() != null && !teacher.getPassword().isEmpty()) {
            teacher.setPassword(encoder.encode(teacher.getPassword()));
        }
        teacherMapper.updateTeacher(teacher);
    }

    @Override
    public void deleteTeacher(String teacherId) {
        teacherMapper.deleteTeacher(teacherId);
    }

    @Override
    public void updateProfile(String teacherId, String name, String password) {
        if (password != null && !password.isBlank()) {
            password = encoder.encode(password);
        }
        teacherMapper.updateProfile(teacherId, name, password);
    }
}
