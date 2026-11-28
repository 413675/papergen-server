package com.papergen.generate.ai;

import com.papergen.generate.exception.BusinessException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 按服务端环境变量配置创建聊天模型，密钥不经过前端、不落库
 */
@Component
public class AiChatModelFactory {

    private static final double DEFAULT_TEMPERATURE = 0.2;

    /** 连接模型服务的超时时间 */
    private static final int CONNECT_TIMEOUT_MILLIS = 20_000;

    /** 单次模型调用的读取超时时间，需小于 Vercel 容器 300 秒上限，让后端先返回可读错误 */
    private static final int READ_TIMEOUT_MILLIS = 290_000;

    @Autowired
    private AiModelProperties properties;

    public ChatModel create() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException("未配置 AI 模型的 API Key，请设置环境变量 ZHIPU_API_KEY");
        }
        String baseUrl = StringUtils.trimTrailingCharacter(properties.getBaseUrl(), '/');
        if (!StringUtils.hasText(baseUrl)
                || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
            throw new BusinessException("AI 模型服务地址必须以 http:// 或 https:// 开头");
        }
        // 长试卷生成耗时较长，显式设置连接与读取超时，避免请求一直挂起
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(properties.getApiKey())
                .completionsPath(resolveCompletionsPath(baseUrl))
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature() == null
                        ? DEFAULT_TEMPERATURE
                        : properties.getTemperature().doubleValue())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * 智谱 GLM 的 OpenAI 兼容端点是 {baseUrl}/chat/completions，
     * 而 Spring AI 默认补全路径是 /v1/chat/completions，直接拼会 404。
     * 未显式覆盖路径时，按 baseUrl 识别智谱并自动纠正。
     */
    private String resolveCompletionsPath(String baseUrl) {
        String configured = properties.getCompletionsPath();
        boolean glm = baseUrl.contains("bigmodel.cn") || baseUrl.contains("z.ai");
        if (glm && (!StringUtils.hasText(configured) || "/v1/chat/completions".equals(configured))) {
            return "/chat/completions";
        }
        return StringUtils.hasText(configured) ? configured : "/v1/chat/completions";
    }
}
