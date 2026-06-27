package jxnu.hc_re.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSimilarity {
    private Integer id;
    private String assignmentName;
    private String studentId1;
    private String studentId2;
    private BigDecimal similarityScore;
    private String detail;
    private Date createdAt;
}
