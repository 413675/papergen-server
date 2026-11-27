package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 判断题表
 * @TableName question_judge
 */
@Schema(description = "判断题表")
@TableName(value ="question_judge")
@Data
public class QuestionJudge extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long questionJudgeId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * true-正确 | false-错误
     */
    @Schema(description = "true-正确 | false-错误")
    private Boolean answer;
}