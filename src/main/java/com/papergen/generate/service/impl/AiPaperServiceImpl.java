package com.papergen.generate.service.impl;

import com.papergen.generate.ai.AiChatModelFactory;
import com.papergen.generate.ai.FileTextExtractor;
import com.papergen.generate.ai.PaperAiWorkflow;
import com.papergen.generate.constant.AiConstant;
import com.papergen.generate.constant.HttpStatus;
import com.papergen.generate.context.PapergenContext;
import com.papergen.generate.domain.entity.AiChatMessage;
import com.papergen.generate.domain.entity.AiModelConfig;
import com.papergen.generate.domain.params.AiModelConfigParam;
import com.papergen.generate.domain.vo.AiChatMessageVO;
import com.papergen.generate.domain.vo.AiChatVO;
import com.papergen.generate.domain.vo.AiModelConfigVO;
import com.papergen.generate.domain.vo.PaperImportVO;
import com.papergen.generate.exception.BusinessException;
import com.papergen.generate.service.AiChatMessageService;
import com.papergen.generate.service.AiModelConfigService;
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
    private AiModelConfigService aiModelConfigService;

    @Autowired
    private AiChatModelFactory aiChatModelFactory;

    @Override
    public AiChatVO chat(String sessionId, String message, MultipartFile file) {
        Long userId = requireUserId();
        requireSessionId(sessionId);
        boolean hasFile = file != null && !file.isEmpty();
        if (!hasFile && !StringUtils.hasText(message)) {
            throw new BusinessException("请输入问题或上传文件");
        }

        String effectiveMessage = StringUtils.hasText(message) ? message.trim() : "请根据上传的文件生成一份试卷";
        List<AiChatMessage> memory = aiChatMessageService.listRecent(userId, sessionId, AiConstant.MAX_MEMORY_MESSAGES);
        String history = buildHistory(memory);
        AiModelConfig modelConfig = aiModelConfigService.getByOwner(userId);
        if (modelConfig == null || !StringUtils.hasText(modelConfig.getApiKey())) {
            throw new BusinessException("请先配置 AI 模型的 API Key 和地址");
        }
        ChatModel chatModel = aiChatModelFactory.create(modelConfig);

        if (!hasFile) {
            // 没有上传文件时只做问答，禁止生成试卷
            saveMessage(userId, sessionId, AiConstant.ROLE_USER, effectiveMessage, null, null, null);
            try {
                String answer = paperAiWorkflow.answer(chatModel, effectiveMessage, history);
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
            PaperImportVO paper = paperAiWorkflow.run(chatModel, fileText, effectiveMessage, history, attachmentName);
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
    public AiModelConfigVO getConfig() {
        Long userId = requireUserId();
        AiModelConfig config = aiModelConfigService.getByOwner(userId);
        if (config == null) {
            AiModelConfigVO vo = new AiModelConfigVO();
            vo.setBaseUrl(AiConstant.DEFAULT_BASE_URL);
            vo.setModel(AiConstant.DEFAULT_MODEL);
            vo.setTemperature(new BigDecimal(AiConstant.DEFAULT_TEMPERATURE));
            vo.setHasApiKey(false);
            return vo;
        }
        return toConfigVO(config);
    }

    @Override
    public AiModelConfigVO saveConfig(AiModelConfigParam param) {
        Long userId = requireUserId();
        if (param == null) {
            throw new BusinessException("配置不能为空");
        }
        AiModelConfig config = aiModelConfigService.getByOwner(userId);
        boolean isNew = config == null;
        if (isNew) {
            config = new AiModelConfig();
            config.setOwnerId(userId);
        }

        String baseUrl = StringUtils.hasText(param.getBaseUrl())
                ? param.getBaseUrl().trim()
                : AiConstant.DEFAULT_BASE_URL;
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new BusinessException("模型地址必须以 http:// 或 https:// 开头");
        }
        String model = StringUtils.hasText(param.getModel())
                ? param.getModel().trim()
                : AiConstant.DEFAULT_MODEL;
        BigDecimal temperature = param.getTemperature() == null
                ? new BigDecimal(AiConstant.DEFAULT_TEMPERATURE)
                : param.getTemperature();
        if (temperature.compareTo(BigDecimal.ZERO) < 0
                || temperature.compareTo(new BigDecimal(AiConstant.MAX_TEMPERATURE)) > 0) {
            throw new BusinessException("温度必须在 0 到 2 之间");
        }

        String apiKey = StringUtils.hasText(param.getApiKey())
                ? param.getApiKey().trim()
                : config.getApiKey();
        if (isNew && !StringUtils.hasText(apiKey)) {
            throw new BusinessException("请填写 API Key");
        }

        config.setBaseUrl(baseUrl);
        config.setModel(model);
        config.setTemperature(temperature);
        config.setApiKey(apiKey);
        aiModelConfigService.saveOrUpdate(config);
        return toConfigVO(config);
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

    private AiModelConfigVO toConfigVO(AiModelConfig config) {
        AiModelConfigVO vo = new AiModelConfigVO();
        vo.setBaseUrl(config.getBaseUrl());
        vo.setModel(config.getModel());
        vo.setTemperature(config.getTemperature());
        vo.setHasApiKey(StringUtils.hasText(config.getApiKey()));
        return vo;
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
