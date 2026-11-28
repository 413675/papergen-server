package com.papergen.generate.service.impl;

import com.papergen.generate.ai.AiChatModelFactory;
import com.papergen.generate.ai.AiProgressTracker;
import com.papergen.generate.ai.FileTextExtractor;
import com.papergen.generate.ai.PaperAiWorkflow;
import com.papergen.generate.constant.AiConstant;
import com.papergen.generate.constant.HttpStatus;
import com.papergen.generate.context.PapergenContext;
import com.papergen.generate.domain.entity.AiChatMessage;
import com.papergen.generate.domain.vo.AiChatMessageVO;
import com.papergen.generate.domain.vo.AiChatVO;
import com.papergen.generate.domain.vo.AiProgressVO;
import com.papergen.generate.domain.vo.PaperImportVO;
import com.papergen.generate.exception.BusinessException;
import com.papergen.generate.service.AiChatMessageService;
import com.papergen.generate.service.AiPaperService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 导卷实现：解析文件、维护对话记忆、驱动工作流
 */
@Service
public class AiPaperServiceImpl implements AiPaperService {

    private static final int HISTORY_LIMIT = 50;

    @Autowired
    private PaperAiWorkflow paperAiWorkflow;

    @Autowired
    private FileTextExtractor fileTextExtractor;

    @Autowired
    private AiChatMessageService aiChatMessageService;

    @Autowired
    private AiChatModelFactory aiChatModelFactory;

    @Autowired
    private AiProgressTracker progressTracker;

    @Override
    public AiChatVO chat(String sessionId, String message, MultipartFile file) {
        Long userId = requireUserId();
        requireSessionId(sessionId);
        boolean hasFile = file != null && !file.isEmpty();
        if (!hasFile && !StringUtils.hasText(message)) {
            throw new BusinessException("请输入问题或上传文件");
        }
        progressTracker.generating(userId, sessionId);
        try {
            return doChat(userId, sessionId, message, file, hasFile);
        } finally {
            progressTracker.clear(userId, sessionId);
        }
    }

    /**
     * 执行对话：没有上传文件时只问答，上传文件时走导卷工作流
     */
    private AiChatVO doChat(Long userId, String sessionId, String message, MultipartFile file, boolean hasFile) {
        String effectiveMessage = StringUtils.hasText(message) ? message.trim() : "请根据上传的文件生成一份试卷";
        List<AiChatMessage> memory = aiChatMessageService.listRecent(userId, sessionId, AiConstant.MAX_MEMORY_MESSAGES);
        String history = buildHistory(memory);
        // 模型配置来自服务端环境变量，密钥不经过前端
        ChatModel chatModel = aiChatModelFactory.create();

        if (!hasFile) {
            // 没有上传文件时只做问答，禁止生成试卷
            saveMessage(userId, sessionId, AiConstant.ROLE_USER, effectiveMessage, null, null, null);
            try {
                String answer = paperAiWorkflow.answer(chatModel, effectiveMessage, history, userId, sessionId);
                saveMessage(userId, sessionId, AiConstant.ROLE_ASSISTANT, answer, null, null, null);
                AiChatVO vo = new AiChatVO();
                vo.setReply(answer);
                return vo;
            } catch (BusinessException e) {
                saveMessage(userId, sessionId, AiConstant.ROLE_ASSISTANT, "回答失败：" + e.getMessage(), null, null, null);
                throw e;
            }
        }

        String attachmentName = file.getOriginalFilename();
        String fileText = fileTextExtractor.extract(file);

        saveMessage(userId, sessionId, AiConstant.ROLE_USER, effectiveMessage,
                attachmentName,
                truncate(fileText, AiConstant.MAX_ATTACHMENT_STORE_LENGTH),
                null);

        try {
            PaperImportVO paper = paperAiWorkflow.run(chatModel, fileText, effectiveMessage, history,
                    attachmentName, userId, sessionId);
            String reply = buildReply(paper);
            saveMessage(userId, sessionId, AiConstant.ROLE_ASSISTANT, reply, null, null, paper.getPaperId());
            AiChatVO vo = new AiChatVO();
            vo.setReply(reply);
            vo.setPaperId(paper.getPaperId());
            vo.setPaperTitle(paper.getTitle());
            vo.setQuestionCount(paper.getQuestionCount());
            vo.setTotalScore(paper.getTotalScore());
            return vo;
        } catch (BusinessException e) {
            saveMessage(userId, sessionId, AiConstant.ROLE_ASSISTANT, "生成失败：" + e.getMessage(), null, null, null);
            throw e;
        }
    }

    @Override
    public List<AiChatMessageVO> history(String sessionId) {
        Long userId = requireUserId();
        requireSessionId(sessionId);
        return aiChatMessageService.listRecent(userId, sessionId, HISTORY_LIMIT).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void clearHistory(String sessionId) {
        Long userId = requireUserId();
        requireSessionId(sessionId);
        aiChatMessageService.clear(userId, sessionId);
    }

    @Override
    public AiProgressVO progress(String sessionId) {
        Long userId = requireUserId();
        requireSessionId(sessionId);
        return progressTracker.get(userId, sessionId);
    }

    private Long requireUserId() {
        Long userId = PapergenContext.getUserId();
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }

    private void requireSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException("会话ID不能为空");
        }
    }

    private void saveMessage(Long userId, String sessionId, String role, String content,
                             String attachmentName, String attachmentText, Long paperId) {
        AiChatMessage message = new AiChatMessage();
        message.setOwnerId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setAttachmentName(attachmentName);
        message.setAttachmentText(attachmentText);
        message.setPaperId(paperId);
        aiChatMessageService.save(message);
    }

    private String buildHistory(List<AiChatMessage> messages) {
        StringBuilder history = new StringBuilder();
        for (AiChatMessage message : messages) {
            String content = truncate(message.getContent(), AiConstant.MAX_MEMORY_CONTENT_LENGTH);
            if (!StringUtils.hasText(content) && StringUtils.hasText(message.getAttachmentName())) {
                content = "（上传文件：" + message.getAttachmentName() + "）";
            }
            if (!StringUtils.hasText(content)) {
                continue;
            }
            String roleLabel = AiConstant.ROLE_USER.equals(message.getRole()) ? "用户" : "助手";
            history.append(roleLabel).append("：").append(content).append('\n');
        }
        return history.toString();
    }

    private String buildReply(PaperImportVO paper) {
        BigDecimal totalScore = paper.getTotalScore() == null ? BigDecimal.ZERO : paper.getTotalScore();
        return "已生成试卷《" + paper.getTitle() + "》，共 "
                + (paper.getQuestionCount() == null ? 0 : paper.getQuestionCount())
                + " 道题，总分 " + totalScore.stripTrailingZeros().toPlainString()
                + " 分。要现在开始答题吗？";
    }

    private AiChatMessageVO toVO(AiChatMessage message) {
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setAiChatMessageId(message.getAiChatMessageId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setAttachmentName(message.getAttachmentName());
        vo.setPaperId(message.getPaperId());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "…";
    }
}
