package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 试卷题目关联表
 * @TableName paper_question
 */
@Schema(description = "试卷题目关联表")
@TableName(value ="paper_question")
@Data
public class PaperQuestion extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long paperQuestionId;

    /**
     * 试卷ID
     */
    @Schema(description = "试卷ID")
    private Long paperId;

    /**
     * 试卷大题ID
     */
    @Schema(description = "试卷大题ID")
    private Long paperPartId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * 排序号
     */
    @Schema(description = "排序号")
    private Integer sortOrder;

    /**
     * 本题分值
     */
    @Schema(description = "本题分值")
    private BigDecimal score;
}