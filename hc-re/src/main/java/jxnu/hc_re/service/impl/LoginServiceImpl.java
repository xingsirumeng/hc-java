package jxnu.hc_re.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jxnu.hc_re.config.JwtUtils;
import jxnu.hc_re.mapper.StudentMapper;
import jxnu.hc_re.mapper.TeacherMapper;
import jxnu.hc_re.pojo.Student;
import jxnu.hc_re.pojo.Teacher;
import jxnu.hc_re.service.LoginService;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> teacherLogin(String teacherId, String password) {
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("账号或密码错误");
        }
        verifyAndUpgradePassword(teacher.getPassword(), password, hashed ->
            teacherMapper.updateTeacherPassword(teacherId, hashed));
        String token = jwtUtils.generateToken(teacherId, teacher.getName(), "teacher");
        return buildResult(token, "teacher", teacherId, teacher.getName());
    }

    @Override
    public Map<String, Object> studentLogin(String studentId, String password) {
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new RuntimeException("账号或密码错误");
        }
        verifyAndUpgradePassword(student.getPassword(), password, hashed ->
            studentMapper.updateStudentPassword(studentId, hashed));
        String token = jwtUtils.generateToken(studentId, student.getName(), "student");
        return buildResult(token, "student", studentId, student.getName());
    }

    // ── 私有方法 ──

    private void verifyAndUpgradePassword(String stored, String input,
                                          java.util.function.Consumer<String> upgrade) {
        if (isBCrypt(stored)) {
            if (!encoder.matches(input, stored)) {
                throw new RuntimeException("账号或密码错误");
            }
        } else {
            if (!input.equals(stored)) {
                throw new RuntimeException("账号或密码错误");
            }
            upgrade.accept(encoder.encode(input));
        }
    }

    private Map<String, Object> buildResult(String token, String role, String id, String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, String> info = Map.of(
            role.equals("teacher") ? "teacherId" : "studentId", id,
            "name", name
        );
        data.put(role, info);
        return data;
    }

    private boolean isBCrypt(String password) {
        return password != null
                && (password.startsWith("$2a$")
                        || password.startsWith("$2b$")
                        || password.startsWith("$2y$"));
    }
}
