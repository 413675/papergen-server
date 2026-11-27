package com.papergen.generate.service;

import com.github.yulichang.base.MPJBaseService;
import com.papergen.generate.domain.entity.AiChatMessage;

import java.util.List;

/**
 * AI 对话消息
 */
public interface AiChatMessageService extends MPJBaseService<AiChatMessage> {

    /**
     * 按时间正序返回指定会话最近若干条消息
     */
    List<AiChatMessage> listRecent(Long ownerId, String sessionId, int limit);

    /**
     * 清空指定会话的对话记忆
     */
    void clear(Long ownerId, String sessionId);
}
