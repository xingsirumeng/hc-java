package jxnu.hc_re.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    private String assignmentName;
    private String studentId;
    private Date submissionTime;
    private String filepath;
    private String originalFilename;
    private Integer score;
    private String comment;
    private Integer aigcScore;
    private String annotatedFilepath;
}
