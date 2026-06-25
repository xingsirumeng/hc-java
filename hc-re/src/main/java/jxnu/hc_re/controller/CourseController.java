package jxnu.hc_re.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jxnu.hc_re.mapper.CourseMapper;
import jxnu.hc_re.pojo.Course;
import jxnu.hc_re.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseMapper courseMapper;

    /** 获取当前教师所教的课程列表（每个课程即一个"班"） */
    @GetMapping("/my")
    public Result<List<Course>> getMyCourses(HttpServletRequest request) {
        String teacherId = (String) request.getAttribute("currentTeacherId");
        List<Course> list = courseMapper.findByTeacherId(teacherId);
        return Result.success(list);
    }
}
