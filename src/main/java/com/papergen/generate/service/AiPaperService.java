package com.papergen.generate.service;

import com.papergen.generate.domain.vo.AiChatMessageVO;
import com.papergen.generate.domain.vo.AiChatVO;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 导卷：文件转 JSON 并生成试卷
 */
public interface AiPaperService {

    /**
     * 与 AI 对话，可携带任意格式的试卷文件；模型配置由前端传入，不落库
     */
    AiChatVO chat(String sessionId, String message, String apiKey,
                  String baseUrl, String model, BigDecimal temperature, MultipartFile file);

    /**
     * 指定会话的对话记忆
     */
    List<AiChatMessageVO> history(String sessionId);

    /**
     * 清空指定会话的对话记忆
     */
    void clearHistory(String sessionId);
}
