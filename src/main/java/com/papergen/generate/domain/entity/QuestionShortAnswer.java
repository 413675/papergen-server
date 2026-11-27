package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

/**
 * 简答题表
 * @TableName question_short_answer
 */
@Schema(description = "简答题表")
@TableName(value ="question_short_answer", autoResultMap = true)
@Data
public class QuestionShortAnswer extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long questionShortAnswerId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * 参考答案
     */
    @Schema(description = "参考答案")
    private String referenceAnswer;

    /**
     * 评分要点JSON
     */
    @Schema(description = "评分要点JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object scoringPoints;
}