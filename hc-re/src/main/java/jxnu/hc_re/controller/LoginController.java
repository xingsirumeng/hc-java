package jxnu.hc_re.controller;

import java.util.Map;

import jxnu.hc_re.pojo.LoginRequest;
import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.pojo.StudentLoginRequest;
import jxnu.hc_re.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            return Result.success(loginService.teacherLogin(
                    request.getTeacherId(), request.getPassword()));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login/student")
    public Result<Map<String, Object>> loginStudent(@RequestBody StudentLoginRequest request) {
        try {
            return Result.success(loginService.studentLogin(
                    request.getStudentId(), request.getPassword()));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
