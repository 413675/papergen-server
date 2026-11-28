package com.papergen.generate.ai;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papergen.generate.constant.AiConstant;
import com.papergen.generate.domain.vo.PaperImportVO;
import com.papergen.generate.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 导卷工作流：文件文本 → 生成试卷 JSON → 自检 → 不合格回炉重生成 → 工具调用建卷
 */
@Slf4j
@Component
public class PaperAiWorkflow {

    private static final String NODE_GENERATE = "generateJson";
    private static final String NODE_VALIDATE = "validateJson";
    private static final String NODE_CREATE = "createPaper";

    private static final String KEY_FILE_TEXT = "fileText";
    private static final String KEY_CHAT_MODEL = "chatModel";
    private static final String KEY_ATTACHMENT_NAME = "attachmentName";
    private static final String KEY_USER_MESSAGE = "userMessage";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_RESPONSE = "modelResponse";
    private static final String KEY_PAPER_JSON = "paperJson";
    private static final String KEY_FEEDBACK = "feedback";
    private static final String KEY_ERROR = "error";
    private static final String KEY_ATTEMPT = "attempt";
    private static final String KEY_PAPER_ID = "paperId";
    private static final String KEY_PAPER_TITLE = "paperTitle";
    private static final String KEY_QUESTION_COUNT = "questionCount";
    private static final String KEY_TOTAL_SCORE = "totalScore";
    private static final String KEY_OWNER_ID = "ownerId";
    private static final String KEY_SESSION_ID = "sessionId";

    private static final List<String> STATE_KEYS = List.of(
            KEY_FILE_TEXT, KEY_CHAT_MODEL, KEY_ATTACHMENT_NAME, KEY_USER_MESSAGE, KEY_HISTORY, KEY_RESPONSE,
            KEY_PAPER_JSON, KEY_FEEDBACK, KEY_ERROR, KEY_ATTEMPT, KEY_PAPER_ID,
            KEY_PAPER_TITLE, KEY_QUESTION_COUNT, KEY_TOTAL_SCORE, KEY_OWNER_ID, KEY_SESSION_ID);

    @Autowired
    private PaperCreateTool paperCreateTool;

    @Autowired
    private AiChatInvoker aiChatInvoker;

    @Autowired
    private ObjectMapper objectMapper;

    private ToolCallback createPaperTool;

    private CompiledGraph graph;

    private String systemPrompt;

    @PostConstruct
    public void init() {
        try {
            this.systemPrompt = loadSystemPrompt();
            this.createPaperTool = Arrays.stream(ToolCallbacks.from(paperCreateTool))
                    .filter(callback -> "createPaper".equals(callback.getToolDefinition().name()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("未找到 createPaper 工具"));
            this.graph = buildGraph();
        } catch (Exception e) {
            throw new IllegalStateException("AI 导卷工作流初始化失败", e);
        }
    }

    /**
     * 执行一次完整工作流，失败时抛出业务异常
     */
    public PaperImportVO run(ChatModel chatModel, String fileText, String userMessage, String history,
                             String attachmentName, Long ownerId, String sessionId) {
        if (!StringUtils.hasText(fileText)) {
            throw new BusinessException("未提供文件内容，无法生成试卷");
        }
        Map<String, Object> input = new java.util.HashMap<>();
        input.put(KEY_CHAT_MODEL, chatModel);
        input.put(KEY_OWNER_ID, ownerId);
        input.put(KEY_SESSION_ID, sessionId);
        input.put(KEY_FILE_TEXT, fileText == null ? "" : fileText);
        input.put(KEY_ATTACHMENT_NAME, attachmentName == null ? "" : attachmentName);
        input.put(KEY_USER_MESSAGE, userMessage == null ? "" : userMessage);
        input.put(KEY_HISTORY, history == null ? "" : history);
        input.put(KEY_RESPONSE, "");
        input.put(KEY_PAPER_JSON, "");
        input.put(KEY_FEEDBACK, "");
        input.put(KEY_ERROR, "");
        input.put(KEY_ATTEMPT, 0);
        input.put(KEY_PAPER_ID, 0L);
        input.put(KEY_PAPER_TITLE, "");
        input.put(KEY_QUESTION_COUNT, 0);
        input.put(KEY_TOTAL_SCORE, BigDecimal.ZERO);

        OverAllState state = graph.invoke(input)
                .orElseThrow(() -> new BusinessException("AI 工作流执行失败"));
        long paperId = state.value(KEY_PAPER_ID, 0L);
        if (paperId <= 0) {
            String error = state.value(KEY_ERROR, "未知错误");
            throw new BusinessException("AI 生成试卷失败：" + error);
        }

        PaperImportVO paper = new PaperImportVO();
        paper.setPaperId(paperId);
        paper.setTitle(state.value(KEY_PAPER_TITLE, ""));
        paper.setQuestionCount(state.value(KEY_QUESTION_COUNT, 0));
        paper.setTotalScore(state.value(KEY_TOTAL_SCORE, BigDecimal.ZERO));
        return paper;
    }

    /**
     * 纯问答：没有上传文件时只回答用户问题，不生成试卷
     */
    public String answer(ChatModel chatModel, String userMessage, String history, Long ownerId, String sessionId) {
        if (chatModel == null) {
            throw new BusinessException("AI 模型未初始化，请检查服务端模型配置");
        }
        StringBuilder prompt = new StringBuilder();
        if (StringUtils.hasText(history)) {
            prompt.append("【历史对话】\n").append(history).append("\n\n");
        }
        prompt.append("【本次问题】\n").append(userMessage == null ? "" : userMessage);
        ChatResponse response;
        try {
            response = aiChatInvoker.call(chatModel, new Prompt(List.of(
                    new SystemMessage(AiConstant.CHAT_SYSTEM_PROMPT),
                    new UserMessage(prompt.toString()))), ownerId, sessionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI 调用失败，请检查模型配置：" + rootMessage(e));
        }
        String output = response.getResult() == null || response.getResult().getOutput() == null
                ? null : response.getResult().getOutput().getText();
        if (!StringUtils.hasText(output)) {
            throw new BusinessException("AI 没有返回内容，请稍后重试");
        }
        return output.trim();
    }

    private CompiledGraph buildGraph() throws Exception {
        StateGraph stateGraph = new StateGraph(() -> {
            OverAllState state = new OverAllState();
            STATE_KEYS.forEach(key -> state.registerKeyAndStrategy(key, new ReplaceStrategy()));
            return state;
        });
        stateGraph.addNode(NODE_GENERATE, AsyncNodeAction.node_async(this::generateNode));
        stateGraph.addNode(NODE_VALIDATE, AsyncNodeAction.node_async(this::validateNode));
        stateGraph.addNode(NODE_CREATE, AsyncNodeAction.node_async(this::createNode));

        stateGraph.addEdge(StateGraph.START, NODE_GENERATE);
        stateGraph.addEdge(NODE_GENERATE, NODE_VALIDATE);
        stateGraph.addConditionalEdges(NODE_VALIDATE, AsyncEdgeAction.edge_async(this::afterValidate),
                Map.of("create", NODE_CREATE, "retry", NODE_GENERATE, "fail", StateGraph.END));
        stateGraph.addConditionalEdges(NODE_CREATE, AsyncEdgeAction.edge_async(this::afterCreate),
                Map.of("done", StateGraph.END, "retry", NODE_GENERATE, "fail", StateGraph.END));
        return stateGraph.compile();
    }

    /**
     * 生成节点：把文件内容、历史对话、用户要求和上一次失败原因交给模型
     */
    private Map<String, Object> generateNode(OverAllState state) {
        ChatModel chatModel = state.value(KEY_CHAT_MODEL, (ChatModel) null);
        if (chatModel == null) {
            throw new BusinessException("AI 模型未初始化，请检查服务端模型配置");
        }
        int attempt = state.value(KEY_ATTEMPT, 0) + 1;
        String userPrompt = buildUserPrompt(state);
        Long ownerId = state.value(KEY_OWNER_ID, 0L);
        String sessionId = state.value(KEY_SESSION_ID, "");
        ChatResponse response;
        long startedAt = System.currentTimeMillis();
        try {
            response = aiChatInvoker.call(chatModel, new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt))), ownerId, sessionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("AI 调用失败，请检查模型配置：" + rootMessage(e));
        }
        String output = response.getResult().getOutput().getText();
        log.info("AI 第 {} 次生成试卷 JSON，耗时 {} ms", attempt, System.currentTimeMillis() - startedAt);
        return Map.of(
                KEY_RESPONSE, output == null ? "" : output,
                KEY_ATTEMPT, attempt,
                KEY_FEEDBACK, "");
    }

    /**
     * 自检节点：校验模型输出是否为合法且结构完整的试卷 JSON
     */
    private Map<String, Object> validateNode(OverAllState state) {
        String response = state.value(KEY_RESPONSE, "");
        try {
            String paperJson = extractJson(response);
            JsonNode root = objectMapper.readTree(paperJson);
            if (root.path("version").asInt() != 1) {
                throw new IllegalArgumentException("version 必须为 1");
            }
            if (!StringUtils.hasText(root.path("paper").path("title").asText(""))) {
                throw new IllegalArgumentException("缺少 paper.title");
            }
            JsonNode parts = root.path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new IllegalArgumentException("parts 必须是非空数组");
            }
            for (JsonNode part : parts) {
                JsonNode questions = part.path("questions");
                if (!questions.isArray() || questions.isEmpty()) {
                    throw new IllegalArgumentException("每个 part 至少需要一道题目");
                }
            }
            return Map.of(KEY_PAPER_JSON, paperJson, KEY_ERROR, "");
        } catch (Exception e) {
            String error = "输出不是合法的试卷 JSON：" + rootMessage(e);
            log.warn("试卷 JSON 自检未通过：{}", error);
            return Map.of(KEY_PAPER_JSON, "", KEY_ERROR, error, KEY_FEEDBACK, error);
        }
    }

    /**
     * 建卷节点：通过工具调用把 JSON 交给导入接口
     */
    private Map<String, Object> createNode(OverAllState state) {
        String paperJson = state.value(KEY_PAPER_JSON, "");
        try {
            // 工具调用的入参需要是参数名到值的 JSON 映射
            String toolInput = objectMapper.writeValueAsString(Map.of("paperJson", paperJson));
            String result = createPaperTool.call(toolInput);
            PaperImportVO paper = objectMapper.readValue(result, PaperImportVO.class);
            log.info("AI 试卷创建成功，paperId={}", paper.getPaperId());
            return Map.of(
                    KEY_PAPER_ID, paper.getPaperId(),
                    KEY_PAPER_TITLE, paper.getTitle() == null ? "" : paper.getTitle(),
                    KEY_QUESTION_COUNT, paper.getQuestionCount() == null ? 0 : paper.getQuestionCount(),
                    KEY_TOTAL_SCORE, paper.getTotalScore() == null ? BigDecimal.ZERO : paper.getTotalScore(),
                    KEY_ERROR, "");
        } catch (Exception e) {
            String error = "试卷保存失败：" + rootMessage(e);
            log.warn("工具调用建卷失败：{}", error);
            return Map.of(KEY_PAPER_ID, 0L, KEY_ERROR, error, KEY_FEEDBACK, error);
        }
    }

    private String afterValidate(OverAllState state) {
        if (!StringUtils.hasText(state.value(KEY_ERROR, ""))) {
            return "create";
        }
        return canRetry(state) ? "retry" : "fail";
    }

    private String afterCreate(OverAllState state) {
        long paperId = state.value(KEY_PAPER_ID, 0L);
        if (paperId > 0) {
            return "done";
        }
        return canRetry(state) ? "retry" : "fail";
    }

    private boolean canRetry(OverAllState state) {
        return state.value(KEY_ATTEMPT, 0) < AiConstant.MAX_GENERATE_ATTEMPTS;
    }

    private String buildUserPrompt(OverAllState state) {
        StringBuilder prompt = new StringBuilder();
        String fileText = state.value(KEY_FILE_TEXT, "");
        if (StringUtils.hasText(fileText)) {
            prompt.append("【文件内容：")
                    .append(state.value(KEY_ATTACHMENT_NAME, "未命名文件"))
                    .append("】\n")
                    .append(fileText)
                    .append("\n\n");
        }
        String history = state.value(KEY_HISTORY, "");
        if (StringUtils.hasText(history)) {
            prompt.append("【历史对话】\n").append(history).append("\n\n");
        }
        // 每次请求附上随机种子，迫使模型重新随机抽题和排序，避免与历史结果雷同
        prompt.append("【最高优先级：先守结构，再随机】必须先严格按照文件给出的题型、题量、每题分值和总分生成，"
                        + "再在每种题型内部随机抽取题目并随机打乱顺序，不得与之前的生成结果重复。本次随机种子：")
                .append(ThreadLocalRandom.current().nextInt(100000))
                .append("。\n\n");
        String userMessage = state.value(KEY_USER_MESSAGE, "");
        prompt.append("【本次要求】\n")
                .append(StringUtils.hasText(userMessage)
                        ? userMessage
                        : "无额外要求，请直接把文件内容整理成一份完整试卷")
                .append("\n\n");
        String feedback = state.value(KEY_FEEDBACK, "");
        if (StringUtils.hasText(feedback)) {
            prompt.append("【上一次输出不合格，请修正后重新输出】\n").append(feedback).append('\n');
        }
        return prompt.toString();
    }

    /**
     * 从模型输出中截取 JSON，容忍 markdown 代码块和多余说明
     */
    private String extractJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("模型没有返回内容");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("没有找到 JSON 对象");
        }
        return raw.substring(start, end + 1);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return StringUtils.hasText(message) ? message : current.getClass().getSimpleName();
    }

    private String loadSystemPrompt() throws Exception {
        String rules = new ClassPathResource("prompts/paper-ai-system.md")
                .getContentAsString(StandardCharsets.UTF_8);
        String template = new ClassPathResource("prompts/paper-import-template.json")
                .getContentAsString(StandardCharsets.UTF_8);
        return rules + "\n\n合法的试卷 JSON 结构示例：\n" + template;
    }
}
