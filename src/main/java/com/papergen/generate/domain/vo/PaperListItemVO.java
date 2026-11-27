package com.papergen.generate.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 试卷列表项，status 为当前用户视角的作答状态。
 */
@Schema(description = "试卷列表项")
@Data
public class PaperListItemVO {

    @Schema(description = "试卷ID")
    private Long paperId;

    @Schema(description = "试卷标题")
    private String title;

    @Schema(description = "学科")
    private String subject;

    @Schema(description = "试卷说明")
    private String description;

    @Schema(description = "题数")
    private Integer questionCount;

    @Schema(description = "总分")
    private BigDecimal totalScore;

    @Schema(description = "not_started-未开始 | doing-进行中 | submitted-已完成")
    private String status;

    @Schema(description = "答卷ID，未开始时为空")
    private Long examRecordId;

    @Schema(description = "得分，仅已完成时返回")
    private BigDecimal score;

    @Schema(description = "开始作答时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedAt;

    @Schema(description = "交卷时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
