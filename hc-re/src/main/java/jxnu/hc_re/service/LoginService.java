package jxnu.hc_re.service;

import java.util.Map;

public interface LoginService {
    /** 教师登录，返回 token + 教师信息 */
    Map<String, Object> teacherLogin(String teacherId, String password);

    /** 学生登录，返回 token + 学生信息 */
    Map<String, Object> studentLogin(String studentId, String password);
}
