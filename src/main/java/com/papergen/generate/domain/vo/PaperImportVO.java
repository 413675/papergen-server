package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 试卷导入结果
 */
@Schema(description = "试卷导入结果")
@Data
public class PaperImportVO {

    @Schema(description = "生成任务ID")
    private Long taskId;

    @Schema(description = "试卷ID")
    private Long paperId;

    @Schema(description = "试卷标题")
    private String title;

    @Schema(description = "学科")
    private String subject;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "试卷总分")
    private BigDecimal totalScore;
}
