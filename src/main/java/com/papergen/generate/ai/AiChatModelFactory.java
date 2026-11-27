package com.papergen.generate.ai;

import com.papergen.generate.domain.entity.AiModelConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 按用户保存的配置动态创建聊天模型，不依赖后端固定配置
 */
@Component
public class AiChatModelFactory {

    private static final double DEFAULT_TEMPERATURE = 0.2;

    public ChatModel create(AiModelConfig config) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .build();
        BigDecimal temperature = config.getTemperature();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(temperature == null ? DEFAULT_TEMPERATURE : temperature.doubleValue())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
