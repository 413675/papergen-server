package com.papergen.generate.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 错题本题目：在完整题目信息上补充错题记录字段
 */
@Schema(description = "错题本题目")
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WrongQuestionVO extends QuestionVO {

    @Schema(description = "错题记录ID")
    private Long wrongQuestionId;

    @Schema(description = "来源试卷ID")
    private Long paperId;

    @Schema(description = "来源试卷名称")
    private String paperTitle;

    @Schema(description = "错误次数")
    private Integer wrongCount;

    @Schema(description = "最近错误时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastWrongTime;

    @Schema(description = "0-未掌握 | 1-已掌握")
    private Integer mastered;

    @Schema(description = "错题笔记")
    private String remark;
}
