package com.papergen.generate.ai;

import com.papergen.generate.constant.AiConstant;
import com.papergen.generate.domain.vo.AiProgressVO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 请求进度登记：只保存在内存中，请求结束立即清除
 */
@Component
public class AiProgressTracker {

    private final Map<String, AiProgressVO> progressMap = new ConcurrentHashMap<>();

    /**
     * 标记为正常生成中
     */
    public void generating(Long ownerId, String sessionId) {
        update(ownerId, sessionId, AiConstant.PROGRESS_GENERATING, 0, 0);
    }

    /**
     * 标记为模型繁忙、等待重试
     */
    public void retrying(Long ownerId, String sessionId, int retry, long waitSeconds) {
        update(ownerId, sessionId, AiConstant.PROGRESS_RETRYING, retry, waitSeconds);
    }

    public AiProgressVO get(Long ownerId, String sessionId) {
        return progressMap.getOrDefault(key(ownerId, sessionId), idle());
    }

    public void clear(Long ownerId, String sessionId) {
        progressMap.remove(key(ownerId, sessionId));
    }

    private void update(Long ownerId, String sessionId, String status, int retry, long waitSeconds) {
        AiProgressVO progress = new AiProgressVO();
        progress.setStatus(status);
        progress.setRetry(retry);
        progress.setMaxRetries(AiConstant.MAX_MODEL_RETRIES);
        progress.setWaitSeconds(waitSeconds);
        progressMap.put(key(ownerId, sessionId), progress);
    }

    private AiProgressVO idle() {
        AiProgressVO progress = new AiProgressVO();
        progress.setStatus(AiConstant.PROGRESS_IDLE);
        progress.setMaxRetries(AiConstant.MAX_MODEL_RETRIES);
        return progress;
    }

    private String key(Long ownerId, String sessionId) {
        return ownerId + ":" + sessionId;
    }
}
