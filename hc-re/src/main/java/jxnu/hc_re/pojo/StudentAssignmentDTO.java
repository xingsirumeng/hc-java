package jxnu.hc_re.pojo;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生端作业视图，包含提交状态和成绩信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssignmentDTO {
    private String assignmentName;
    private String courseId;
    private String courseName;
    private Date startTime;
    private Date endTime;
    private String requirement;
    private boolean submitted;
    private Date submissionTime;
    private String filepath;
    private String originalFilename;
    private Integer score;
    private String comment;
    private Integer aigcScore;
    private String annotatedFilepath;
    private String status;
    private String returnReason;
    private String gradeAttachments;
}
