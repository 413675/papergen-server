package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型配置（不返回真实密钥）
 */
@Schema(description = "AI 模型配置")
@Data
public class AiModelConfigVO {

    @Schema(description = "模型服务地址")
    private String baseUrl;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "温度")
    private BigDecimal temperature;

    @Schema(description = "是否已配置密钥")
    private Boolean hasApiKey;
}
