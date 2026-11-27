package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.util.Date;
import lombok.Data;

/**
 * 生成任务表
 * @TableName generation_task
 */
@Schema(description = "生成任务表")
@TableName(value ="generation_task", autoResultMap = true)
@Data
public class GenerationTask extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long generationTaskId;

    /**
     * 所属用户ID
     */
    @Schema(description = "所属用户ID")
    private Long ownerId;

    /**
     * 源文档ID
     */
    @Schema(description = "源文档ID")
    private Long documentId;

    /**
     * 试卷ID
     */
    @Schema(description = "试卷ID")
    private Long paperId;

    /**
     * extract-抽题 | generate-AI出题 | assemble-组卷 | import-导入
     */
    @Schema(description = "extract-抽题 | generate-AI出题 | assemble-组卷 | import-导入")
    private String taskType;

    /**
     * pending-待处理 | processing-处理中 | succeeded-成功 | failed-失败
     */
    @Schema(description = "pending-待处理 | processing-处理中 | succeeded-成功 | failed-失败")
    private String status;

    /**
     * 进度0-100
     */
    @Schema(description = "进度0-100")
    private Integer progress;

    /**
     * 任务入参JSON
     */
    @Schema(description = "任务入参JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object params;

    /**
     * 任务结果JSON
     */
    @Schema(description = "任务结果JSON")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object result;

    /**
     * 失败原因
     */
    @Schema(description = "失败原因")
    private String errorMessage;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedAt;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishedAt;
}