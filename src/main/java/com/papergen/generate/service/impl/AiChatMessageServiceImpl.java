package com.papergen.generate.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.papergen.generate.domain.entity.AiChatMessage;
import com.papergen.generate.mapper.AiChatMessageMapper;
import com.papergen.generate.service.AiChatMessageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话消息实现
 */
@Service
public class AiChatMessageServiceImpl extends MPJBaseServiceImpl<AiChatMessageMapper, AiChatMessage>
        implements AiChatMessageService {

    @Override
    public List<AiChatMessage> listRecent(Long ownerId, String sessionId, int limit) {
        if (ownerId == null || !StringUtils.hasText(sessionId) || limit <= 0) {
            return Collections.emptyList();
        }
        List<AiChatMessage> messages = lambdaQuery()
                .eq(AiChatMessage::getOwnerId, ownerId)
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getAiChatMessageId)
                .last("limit " + limit)
                .list();
        List<AiChatMessage> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);
        return ordered;
    }

    @Override
    public void clear(Long ownerId, String sessionId) {
        if (ownerId == null || !StringUtils.hasText(sessionId)) {
            return;
        }
        lambdaUpdate()
                .eq(AiChatMessage::getOwnerId, ownerId)
                .eq(AiChatMessage::getSessionId, sessionId)
                .remove();
    }
}
