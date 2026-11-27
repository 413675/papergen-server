package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 对话结果
 */
@Schema(description = "AI 对话结果")
@Data
public class AiChatVO {

    @Schema(description = "助手回复")
    private String reply;

    @Schema(description = "生成的试卷ID，未生成时为空")
    private Long paperId;

    @Schema(description = "试卷标题")
    private String paperTitle;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "试卷总分")
    private BigDecimal totalScore;
}
