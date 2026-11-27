package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 试卷表
 * @TableName paper
 */
@Schema(description = "试卷表")
@TableName(value ="paper", autoResultMap = true)
@Data
public class Paper extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long paperId;

    /**
     * 所属用户ID
     */
    @Schema(description = "所属用户ID")
    private Long ownerId;

    /**
     * 试卷标题
     */
    @Schema(description = "试卷标题")
    private String title;

    /**
     * 试卷说明
     */
    @Schema(description = "试卷说明")
    private String description;

    /**
     * 学科
     */
    @Schema(description = "学科")
    private String subject;

    /**
     * draft-草稿 | published-已发布 | archived-已归档
     */
    @Schema(description = "draft-草稿 | published-已发布 | archived-已归档")
    private String status;

    /**
     * 总分
     */
    @Schema(description = "总分")
    private BigDecimal totalScore;

    /**
     * 题数
     */
    @Schema(description = "题数")
    private Integer questionCount;

    /**
     * 组卷配置JSON
     */
    @Schema(description = "组卷配置JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object settings;


    /**
     * 发布时间
     */
    @Schema(description = "发布时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishedAt;
}