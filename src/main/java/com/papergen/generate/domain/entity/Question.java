package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 题目公共表
 * @TableName question
 */
@Schema(description = "题目公共表")
@TableName(value ="question", autoResultMap = true)
@Data
public class Question extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long questionId;

    /**
     * 所属用户ID
     */
    @Schema(description = "所属用户ID")
    private Long ownerId;

    /**
     * 来源文档ID，必填，题目只能来自文件导入
     */
    @Schema(description = "来源文档ID，必填，题目只能来自文件导入")
    private Long sourceDocumentId;

    /**
     * 学科
     */
    @Schema(description = "学科")
    private String subject;

    /**
     * choice-选择题 | blank-填空题 | judge-判断题 | short_answer-简答题 | comprehensive-大题
     */
    @Schema(description = "choice-选择题 | blank-填空题 | judge-判断题 | short_answer-简答题 | comprehensive-大题")
    private String type;

    /**
     * 题干JSON
     */
    @Schema(description = "题干JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object stem;

    /**
     * 题干明文
     */
    @Schema(description = "题干明文")
    private String stemText;

    /**
     * 答案解析
     */
    @Schema(description = "答案解析")
    private String analysis;

    /**
     * 默认分值
     */
    @Schema(description = "默认分值")
    private BigDecimal defaultScore;
}