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
 * 选择题表
 * @TableName question_choice
 */
@Schema(description = "选择题表")
@TableName(value ="question_choice", autoResultMap = true)
@Data
public class QuestionChoice extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long questionChoiceId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * single-单选 | multiple-多选
     */
    @Schema(description = "single-单选 | multiple-多选")
    private String choiceMode;

    /**
     * 选项JSON
     */
    @Schema(description = "选项JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object options;

    /**
     * 答案JSON
     */
    @Schema(description = "答案JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object answer;
}