package jxnu.hc_re.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadlineExtension {
    private Integer id;
    private String assignmentName;
    private String studentId;
    private Date extendedEndTime;
    private String reason;
    private Date createdAt;
}
