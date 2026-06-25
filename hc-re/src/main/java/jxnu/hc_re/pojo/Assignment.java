package jxnu.hc_re.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {
    private String name;
    private Date startTime;
    private Date endTime;
    private String courseId;
    private String requirement;
}
