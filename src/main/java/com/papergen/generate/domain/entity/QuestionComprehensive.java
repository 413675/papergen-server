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
 * 大题表
 * @TableName question_comprehensive
 */
@Schema(description = "大题表")
@TableName(value ="question_comprehensive", autoResultMap = true)
@Data
public class QuestionComprehensive extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long questionComprehensiveId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * 材料/背景JSON
     */
    @Schema(description = "材料/背景JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object material;

    /**
     * 小题JSON
     */
    @Schema(description = "小题JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object subQuestions;

    /**
     * 参考答案
     */
    @Schema(description = "参考答案")
    private String referenceAnswer;
}