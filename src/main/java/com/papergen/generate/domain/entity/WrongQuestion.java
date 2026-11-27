package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 错题本表
 * @TableName wrong_question
 */
@Schema(description = "错题本表")
@TableName(value ="wrong_question")
@Data
public class WrongQuestion extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long wrongQuestionId;

    /**
     * 所属用户ID
     */
    @Schema(description = "所属用户ID")
    private Long ownerId;

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    private Long questionId;

    /**
     * 来源试卷ID
     */
    @Schema(description = "来源试卷ID")
    private Long paperId;

    /**
     * 错误次数
     */
    @Schema(description = "错误次数")
    private Integer wrongCount;

    /**
     * 最近错误时间
     */
    @Schema(description = "最近错误时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastWrongTime;

    /**
     * 0-未掌握 | 1-已掌握
     */
    @Schema(description = "0-未掌握 | 1-已掌握")
    private Integer mastered;

    /**
     * 错题笔记
     */
    @Schema(description = "错题笔记")
    private String remark;
}