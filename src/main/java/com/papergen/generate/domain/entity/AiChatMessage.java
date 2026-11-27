package com.papergen.generate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 对话消息表
 */
@Schema(description = "AI 对话消息表")
@TableName("ai_chat_message")
@Data
public class AiChatMessage extends BaseModel {

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long aiChatMessageId;

    @Schema(description = "所属用户ID")
    private Long ownerId;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "user-用户 | assistant-助手")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "附件文件名")
    private String attachmentName;

    @Schema(description = "附件解析后的文本")
    private String attachmentText;

    @Schema(description = "生成/关联的试卷ID")
    private Long paperId;
}
