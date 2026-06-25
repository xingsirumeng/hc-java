package jxnu.hc_re.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jxnu.hc_re.pojo.Teacher;
import jxnu.hc_re.pojo.Result;
import jxnu.hc_re.service.TeacherService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;
    @GetMapping
    public Result<List<Teacher>> teacher() {
        List<Teacher> teachers = teacherService.findAllTeachers();
        return Result.success(teachers);
    }

    @PostMapping("/add")
    public Result<String> addTeacher(@RequestBody Teacher teacher) {
        teacherService.addTeacher(teacher);
        return Result.success("Teacher added successfully");
    }

    @GetMapping("/{teacherId}")
    public Result<Teacher> getTeacher(@PathVariable String teacherId) {
        Teacher teacher = teacherService.findById(teacherId);
        if (teacher == null) {
            return Result.error("教师不存在");
        }
        return Result.success(teacher);
    }

    @PutMapping("/{teacherId}")
    public Result<String> updateTeacher(@PathVariable String teacherId, @RequestBody Teacher teacher) {
        teacher.setTeacherId(teacherId);
        teacherService.updateTeacher(teacher);
        return Result.success("Teacher updated successfully");
    }

    @DeleteMapping("/{teacherId}")
    public Result<String> deleteTeacher(@PathVariable String teacherId) {
        teacherService.deleteTeacher(teacherId);
        return Result.success("Teacher deleted successfully");
    }

    // ── 教师个人资料（自身操作） ──

    /** 获取当前登录教师的个人资料 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(HttpServletRequest request) {
        String teacherId = (String) request.getAttribute("currentTeacherId");
        Teacher teacher = teacherService.findById(teacherId);
        if (teacher == null) {
            return Result.error("教师不存在");
        }
        Map<String, Object> profile = Map.of(
                "teacherId", teacher.getTeacherId(),
                "name", teacher.getName());
        return Result.success(profile);
    }

    /** 更新当前登录教师的个人资料 */
    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        String teacherId = (String) request.getAttribute("currentTeacherId");
        String name = (String) body.get("name");
        String password = (String) body.get("password");

        if ((name == null || name.isBlank()) && (password == null || password.isBlank())) {
            return Result.error("至少需要提供姓名或密码");
        }

        teacherService.updateProfile(teacherId, name, password);
        return Result.success("资料更新成功");
    }
}
