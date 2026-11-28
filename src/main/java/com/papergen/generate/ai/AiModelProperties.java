package com.papergen.generate.ai;

import com.papergen.generate.constant.AiConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * AI 模型配置：沿用 spring.ai.openai 配置项，密钥通过环境变量注入，不进入代码、接口与数据库
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.openai")
public class AiModelProperties {

    private String baseUrl = AiConstant.DEFAULT_BASE_URL;

    private String apiKey;

    private String model = AiConstant.DEFAULT_MODEL;

    /** 聊天补全路径，智谱为 /api/paas/v4/chat/completions */
    private String completionsPath = "/v1/chat/completions";

    private BigDecimal temperature = new BigDecimal(AiConstant.DEFAULT_TEMPERATURE);
}
