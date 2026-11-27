package com.papergen.generate.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * AI 对话消息
 */
@Schema(description = "AI 对话消息")
@Data
public class AiChatMessageVO {

    @Schema(description = "消息ID")
    private Long aiChatMessageId;

    @Schema(description = "user-用户 | assistant-助手")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "附件文件名")
    private String attachmentName;

    @Schema(description = "生成的试卷ID")
    private Long paperId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
