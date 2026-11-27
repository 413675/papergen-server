package com.papergen.generate.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 答卷详情。进行中只含题干，已提交额外返回答案、解析与判分结果。
 */
@Schema(description = "答卷详情")
@Data
public class ExamDetailVO {

    @Schema(description = "答卷ID")
    private Long examRecordId;

    @Schema(description = "试卷ID")
    private Long paperId;

    @Schema(description = "试卷标题")
    private String title;

    @Schema(description = "试卷说明")
    private String description;

    @Schema(description = "学科")
    private String subject;

    @Schema(description = "doing-进行中 | submitted-已提交")
    private String status;

    @Schema(description = "卷面总分")
    private BigDecimal totalScore;

    @Schema(description = "得分")
    private BigDecimal score;

    @Schema(description = "开始作答时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedAt;

    @Schema(description = "交卷时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedAt;

    @Schema(description = "大题列表")
    private List<ExamPartVO> parts;
}
