package com.papergen.generate.domain.params;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型配置参数
 */
@Schema(description = "AI 模型配置参数")
@Data
public class AiModelConfigParam {

    @Schema(description = "模型服务地址，需兼容 OpenAI 协议")
    private String baseUrl;

    @Schema(description = "访问密钥，留空表示不修改已保存的密钥")
    private String apiKey;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "温度，0-2")
    private BigDecimal temperature;
}
