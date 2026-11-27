package com.papergen.generate.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 按请求传入的模型配置动态创建聊天模型，配置不落库、不依赖后端固定配置
 */
@Component
public class AiChatModelFactory {

    private static final double DEFAULT_TEMPERATURE = 0.2;

    public ChatModel create(String baseUrl, String apiKey, String model, BigDecimal temperature) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature == null ? DEFAULT_TEMPERATURE : temperature.doubleValue())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
