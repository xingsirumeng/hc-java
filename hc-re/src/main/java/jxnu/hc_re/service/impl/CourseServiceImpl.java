package jxnu.hc_re.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jxnu.hc_re.mapper.CourseMapper;
import jxnu.hc_re.pojo.Course;
import jxnu.hc_re.service.CourseService;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Override
    public List<Course> findAll() {
        return courseMapper.findAllCourses();
    }

    @Override
    public Course findById(String courseId) {
        return courseMapper.findById(courseId);
    }

    @Override
    public void addCourse(Course course) {
        courseMapper.addCourse(course);
    }

    @Override
    public void updateCourse(Course course) {
        courseMapper.updateCourse(course);
    }

    @Override
    public void deleteCourse(String courseId) {
        courseMapper.deleteCourse(courseId);
    }
}
