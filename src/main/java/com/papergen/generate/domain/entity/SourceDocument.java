package com.papergen.generate.domain.entity;

import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 上传源文件表
 * @TableName source_document
 */
@Schema(description = "上传源文件表")
@TableName(value ="source_document")
@Data
public class SourceDocument extends BaseModel {
    /**
     * 主键
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long sourceDocumentId;

    /**
     * 所属用户ID
     */
    @Schema(description = "所属用户ID")
    private Long ownerId;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 对象存储键
     */
    @Schema(description = "对象存储键")
    private String objectKey;

    /**
     * r2-R2 | local-本地 | inline-纯文本
     */
    @Schema(description = "r2-R2 | local-本地 | inline-纯文本")
    private String storage;

    /**
     * MIME类型
     */
    @Schema(description = "MIME类型")
    private String mimeType;

    /**
     * 文件字节数
     */
    @Schema(description = "文件字节数")
    private Long fileSize;

    /**
     * 页数
     */
    @Schema(description = "页数")
    private Integer pageCount;

    /**
     * 解析后的纯文本
     */
    @Schema(description = "解析后的纯文本")
    private String contentText;

    /**
     * uploaded-已上传 | parsing-解析中 | parsed-已解析 | failed-失败
     */
    @Schema(description = "uploaded-已上传 | parsing-解析中 | parsed-已解析 | failed-失败")
    private String status;

    /**
     * 失败原因
     */
    @Schema(description = "失败原因")
    private String errorMessage;
}