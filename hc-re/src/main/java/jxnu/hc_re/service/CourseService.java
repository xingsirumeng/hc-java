package jxnu.hc_re.service;

import java.util.List;

import jxnu.hc_re.pojo.Course;

public interface CourseService {
    List<Course> findAll();
    Course findById(String courseId);
    void addCourse(Course course);
    void updateCourse(Course course);
    void deleteCourse(String courseId);
}
