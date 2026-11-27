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
 * 答题明细表
 * @TableName exam_answer
 */
@Schema(description = "答题明细表")
@TableName(value ="exam_answer", autoResultMap = true)
@Data
public class ExamAnswer extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long examAnswerId;

    /**
     * 答卷ID
     */
    @Schema(description = "答卷ID")
    private Long examRecordId;

    /**
     * 试卷题目ID
     */
    @Schema(description = "试卷题目ID")
    private Long paperQuestionId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * 题干/选项/答案快照JSON
     */
    @Schema(description = "题干/选项/答案快照JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object questionSnapshot;

    /**
     * 用户作答JSON
     */
    @Schema(description = "用户作答JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object userAnswer;

    /**
     * 0-错误 | 1-正确 | 2-待批改
     */
    @Schema(description = "0-错误 | 1-正确 | 2-待批改")
    private Integer isCorrect;

    /**
     * 本题得分
     */
    @Schema(description = "本题得分")
    private BigDecimal score;
}