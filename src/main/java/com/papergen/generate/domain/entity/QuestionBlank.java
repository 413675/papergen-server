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
 * 填空题表
 * @TableName question_blank
 */
@Schema(description = "填空题表")
@TableName(value ="question_blank", autoResultMap = true)
@Data
public class QuestionBlank extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long questionBlankId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * 空格数量
     */
    @Schema(description = "空格数量")
    private Integer blankCount;

    /**
     * 答案JSON
     */
    @Schema(description = "答案JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object answer;
}