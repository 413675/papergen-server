package com.papergen.generate.service;

import com.papergen.generate.domain.vo.AiChatMessageVO;
import com.papergen.generate.domain.vo.AiChatVO;
import com.papergen.generate.domain.vo.AiProgressVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 导卷：文件转 JSON 并生成试卷
 */
public interface AiPaperService {

    /**
     * 与 AI 对话，可携带任意格式的试卷文件；模型配置来自服务端环境变量
     */
    AiChatVO chat(String sessionId, String message, MultipartFile file);

    /**
     * 指定会话的对话记忆
     */
    List<AiChatMessageVO> history(String sessionId);

    /**
     * 清空指定会话的对话记忆
     */
    void clearHistory(String sessionId);

    /**
     * 当前会话的 AI 请求进度，用于前端展示是否正在重试
     */
    AiProgressVO progress(String sessionId);
}
