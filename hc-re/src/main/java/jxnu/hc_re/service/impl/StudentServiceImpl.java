package jxnu.hc_re.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jxnu.hc_re.mapper.StudentMapper;
import jxnu.hc_re.pojo.Student;
import jxnu.hc_re.service.StudentService;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public List<Student> getStudentsByCourseId(String courseId) {
        return studentMapper.findByCourseId(courseId);
    }

    @Override
    public List<Student> findAll() {
        return studentMapper.findAllStudents();
    }

    @Override
    public Student findById(String studentId) {
        return studentMapper.findById(studentId);
    }

    @Override
    public void updateProfile(String studentId, String name, String newPassword) {
        if (newPassword != null && !newPassword.isBlank()) {
            newPassword = encoder.encode(newPassword);
        }
        studentMapper.updateProfile(studentId, name, newPassword);
    }

    @Override
    public void addStudent(Student student) {
        student.setPassword(encoder.encode(student.getPassword()));
        studentMapper.addStudent(student);
    }

    @Override
    public void updateStudent(Student student) {
        if (student.getPassword() != null && !student.getPassword().isEmpty()) {
            student.setPassword(encoder.encode(student.getPassword()));
        }
        studentMapper.updateStudent(student);
    }

    @Override
    public void deleteStudent(String studentId) {
        studentMapper.deleteStudent(studentId);
    }
}
