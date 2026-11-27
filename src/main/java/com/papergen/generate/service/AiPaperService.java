package com.papergen.generate.service;

import com.papergen.generate.domain.params.AiModelConfigParam;
import com.papergen.generate.domain.vo.AiChatMessageVO;
import com.papergen.generate.domain.vo.AiChatVO;
import com.papergen.generate.domain.vo.AiModelConfigVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 导卷：文件转 JSON 并生成试卷
 */
public interface AiPaperService {

    /**
     * 与 AI 对话，可携带任意格式的试卷文件
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
     * 当前用户的模型配置，密钥只返回是否已配置
     */
    AiModelConfigVO getConfig();

    /**
     * 保存当前用户的模型配置，密钥留空表示不修改
     */
    AiModelConfigVO saveConfig(AiModelConfigParam param);
}
