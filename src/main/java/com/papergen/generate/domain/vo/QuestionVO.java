package com.papergen.generate.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 答题/解析中的题目。答案、解析、参考答案等字段仅在已提交时填充，
 * 进行中与未填写的字段由全局 non_null 策略自动隐藏。
 */
@Schema(description = "试卷题目")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionVO {

    @Schema(description = "试卷题目ID")
    private Long paperQuestionId;

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "题型：choice/blank/judge/short_answer/comprehensive")
    private String type;

    @Schema(description = "学科")
    private String subject;

    @Schema(description = "题干")
    private String stem;

    @Schema(description = "本题分值")
    private BigDecimal score;

    @Schema(description = "选择题：single-单选 | multiple-多选")
    private String choiceMode;

    @Schema(description = "选择题选项：[{key, content}]")
    private List<Map<String, Object>> options;

    @Schema(description = "填空题空格数量")
    private Integer blankCount;

    @Schema(description = "综合题材料")
    private String material;

    @Schema(description = "综合题小题")
    private List<Map<String, Object>> subQuestions;

    @Schema(description = "答案解析，仅已提交")
    private String analysis;

    @Schema(description = "正确答案，仅已提交")
    private Object answer;

    @Schema(description = "主观题参考答案，仅已提交")
    private String referenceAnswer;

    @Schema(description = "主观题评分要点，仅已提交")
    private Object scoringPoints;

    @Schema(description = "用户作答")
    private Object userAnswer;

    @Schema(description = "0-错误 | 1-正确 | 2-待批改，仅已提交")
    private Integer isCorrect;

    @Schema(description = "本题得分，仅已提交")
    private BigDecimal earnedScore;
}
