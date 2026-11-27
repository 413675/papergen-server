package com.papergen.generate;

import com.papergen.generate.ai.AiChatModelFactory;
import com.papergen.generate.constant.UserConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.papergen.generate.domain.entity.AiChatMessage;
import com.papergen.generate.domain.entity.AiModelConfig;
import com.papergen.generate.domain.entity.SysUser;
import com.papergen.generate.mapper.AiChatMessageMapper;
import com.papergen.generate.service.AiModelConfigService;
import com.papergen.generate.service.SysUserService;
import com.papergen.generate.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 导卷集成测试：mock 模型输出，验证文件解析、JSON 自检、工具建卷与对话记忆。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiPaperIntegrationTest {

    /** 模型应返回的合法试卷 JSON */
    private static final String VALID_JSON = """
            {
              "version": 1,
              "paper": {"title": "AI 生成测试卷", "subject": "Linux"},
              "parts": [
                {
                  "title": "一、选择题",
                  "questions": [
                    {
                      "type": "choice",
                      "choiceMode": "single",
                      "stem": "查看当前进程的命令是？",
                      "options": [
                        {"key": "A", "content": "ps"},
                        {"key": "B", "content": "ls"}
                      ],
                      "answer": ["A"],
                      "analysis": "ps 用于查看进程。",
                      "score": 2
                    }
                  ]
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AiChatMessageMapper aiChatMessageMapper;

    @Autowired
    private AiModelConfigService aiModelConfigService;

    @MockitoBean
    private AiChatModelFactory aiChatModelFactory;

    private ChatModel chatModel;

    private String token;

    private Long testerUserId;

    @BeforeEach
    void setUp() {
        SysUser user = new SysUser();
        user.setUsername("ai_tester");
        user.setPasswordHash("not-a-real-hash");
        user.setDisplayName("AI 测试");
        user.setRole(UserConstant.DEFAULT_ROLE);
        user.setStatus(UserConstant.STATUS_ENABLED);
        sysUserService.save(user);
        testerUserId = user.getSysUserId();
        token = jwtUtil.generateToken(user);

        AiModelConfig config = new AiModelConfig();
        config.setOwnerId(testerUserId);
        config.setBaseUrl("https://api.deepseek.com");
        config.setApiKey("test-key");
        config.setModel("deepseek-chat");
        config.setTemperature(new BigDecimal("0.2"));
        aiModelConfigService.save(config);

        chatModel = Mockito.mock(ChatModel.class);
        when(aiChatModelFactory.create(any(AiModelConfig.class))).thenReturn(chatModel);
    }

    @Test
    void uploadFileShouldGeneratePaperAndRememberMessages() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(VALID_JSON));
        MockMultipartFile file = new MockMultipartFile(
                "file", "linux.txt", "text/plain",
                "选择题：查看当前进程的命令是？A. ps  B. ls".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/v1/ai/chat")
                        .file(file)
                        .param("sessionId", "session-test-1")
                        .param("message", "整理成可答题的试卷")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paperId").isNumber())
                .andExpect(jsonPath("$.data.paperTitle").value("AI 生成测试卷"))
                .andExpect(jsonPath("$.data.questionCount").value(1));

        List<AiChatMessage> messages = aiChatMessageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>().eq(AiChatMessage::getOwnerId, testerUserId));
        assertEquals(2, messages.size());
        AiChatMessage assistant = messages.stream()
                .filter(item -> "assistant".equals(item.getRole()))
                .findFirst()
                .orElseThrow();
        assertNotNull(assistant.getPaperId());

        mockMvc.perform(get("/v1/ai/messages")
                        .param("sessionId", "session-test-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void invalidJsonShouldBeReflectedAndRegenerated() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(chatResponse("抱歉，我无法生成"))
                .thenReturn(chatResponse(VALID_JSON));
        MockMultipartFile file = new MockMultipartFile(
                "file", "linux.md", "text/markdown",
                "# Linux 测试题".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/v1/ai/chat")
                        .file(file)
                        .param("sessionId", "session-test-2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paperId").isNumber());

        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void messageWithoutFileShouldOnlyAnswer() throws Exception {
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("这是普通问答回复"));
        mockMvc.perform(multipart("/v1/ai/chat")
                        .param("sessionId", "session-test-3")
                        .param("message", "什么是指针？")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.reply").value("这是普通问答回复"))
                .andExpect(jsonPath("$.data.paperId").doesNotExist());

        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
