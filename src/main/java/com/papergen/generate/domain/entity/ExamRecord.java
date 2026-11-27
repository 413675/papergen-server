package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 答卷表（一次作答）
 * @TableName exam_record
 */
@Schema(description = "答卷表（一次作答）")
@TableName(value ="exam_record")
@Data
public class ExamRecord extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long examRecordId;

    /**
     * 试卷ID
     */
    @Schema(description = "试卷ID")
    private Long paperId;

    /**
     * 作答用户ID
     */
    @Schema(description = "作答用户ID")
    private Long ownerId;

    /**
     * doing-进行中 | submitted-已提交
     */
    @Schema(description = "doing-进行中 | submitted-已提交")
    private String status;

    /**
     * 得分
     */
    @Schema(description = "得分")
    private BigDecimal score;

    /**
     * 卷面总分
     */
    @Schema(description = "卷面总分")
    private BigDecimal totalScore;

    /**
     * 开始作答时间
     */
    @Schema(description = "开始作答时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startedAt;

    /**
     * 交卷时间
     */
    @Schema(description = "交卷时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedAt;
}