package jxnu.hc_re.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jxnu.hc_re.mapper.ScMapper;
import jxnu.hc_re.pojo.Sc;
import jxnu.hc_re.service.ScService;

@Service
public class ScServiceImpl implements ScService {

    @Autowired
    private ScMapper scMapper;

    @Override
    public List<Sc> findAll() {
        return scMapper.findAll();
    }

    @Override
    public List<Sc> findByCourseId(String courseId) {
        return scMapper.findByCourseId(courseId);
    }

    @Override
    public List<Sc> findByStudentId(String studentId) {
        return scMapper.findByStudentId(studentId);
    }

    @Override
    public void enroll(String studentId, String courseId) {
        Sc sc = new Sc();
        sc.setStudentId(studentId);
        sc.setCourseId(courseId);
        scMapper.insert(sc);
    }

    @Override
    public void unenroll(Integer scId) {
        scMapper.deleteById(scId);
    }

    @Override
    public void unenrollByStudentAndCourse(String studentId, String courseId) {
        scMapper.deleteByStudentAndCourse(studentId, courseId);
    }
}
