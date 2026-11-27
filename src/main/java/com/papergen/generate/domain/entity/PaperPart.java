package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 试卷大题（部分）表
 * @TableName paper_part
 */
@Schema(description = "试卷大题（部分）表")
@TableName(value ="paper_part")
@Data
public class PaperPart extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long paperPartId;

    /**
     * 试卷ID
     */
    @Schema(description = "试卷ID")
    private Long paperId;

    /**
     * 大题标题
     */
    @Schema(description = "大题标题")
    private String title;

    /**
     * 答题说明
     */
    @Schema(description = "答题说明")
    private String instruction;

    /**
     * 排序号
     */
    @Schema(description = "排序号")
    private Integer sortOrder;
}