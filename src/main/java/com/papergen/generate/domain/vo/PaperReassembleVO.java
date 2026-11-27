package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 错题重组试卷结果
 */
@Schema(description = "错题重组试卷结果")
@Data
public class PaperReassembleVO {

    @Schema(description = "新试卷ID")
    private Long paperId;

    @Schema(description = "试卷名称")
    private String title;

    @Schema(description = "科目")
    private String subject;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "总分")
    private BigDecimal totalScore;
}
