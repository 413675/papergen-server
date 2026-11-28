package com.papergen.generate.ai;

import com.papergen.generate.constant.AiConstant;
import com.papergen.generate.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * 模型调用入口：模型繁忙（429 / 智谱 1305）时按 5s、10s、20s 自动退避重试
 */
@Slf4j
@Component
public class AiChatInvoker {

    private static final long[] RETRY_DELAY_SECONDS = {5, 10, 20};

    @Autowired
    private AiProgressTracker progressTracker;

    /**
     * 调用模型；重试次数用尽仍繁忙时抛出友好提示
     */
    public ChatResponse call(ChatModel chatModel, Prompt prompt, Long ownerId, String sessionId) {
        int maxRetries = RETRY_DELAY_SECONDS.length;
        for (int retry = 0; ; retry++) {
            try {
                return chatModel.call(prompt);
            } catch (Exception e) {
                boolean overloaded = isOverloaded(e);
                if (!overloaded || retry >= maxRetries) {
                    if (overloaded) {
                        throw new BusinessException("当前模型访问量过大，请稍后重试");
                    }
                    throw e;
                }
                long waitSeconds = RETRY_DELAY_SECONDS[retry];
                log.warn("模型访问量过大，{} 秒后进行第 {}/{} 次重试", waitSeconds, retry + 1, maxRetries);
                progressTracker.retrying(ownerId, sessionId, retry + 1, waitSeconds);
                sleep(waitSeconds);
                progressTracker.generating(ownerId, sessionId);
            }
        }
    }

    /**
     * 判断异常是否为模型繁忙：HTTP 429 或智谱错误码 1305
     */
    private boolean isOverloaded(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException responseException
                    && responseException.getStatusCode().value() == 429) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("1305") || message.contains("访问量过大"))) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("AI 调用已中断，请稍后重试");
        }
    }
}
