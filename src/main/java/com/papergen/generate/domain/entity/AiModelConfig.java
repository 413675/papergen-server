package com.papergen.generate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.papergen.generate.domain.model.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型配置表
 */
@Schema(description = "AI 模型配置表")
@TableName("ai_model_config")
@Data
public class AiModelConfig extends BaseModel {

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long aiModelConfigId;

    @Schema(description = "所属用户ID")
    private Long ownerId;

    @Schema(description = "模型服务地址，兼容 OpenAI 协议")
    private String baseUrl;

    @Schema(description = "访问密钥")
    private String apiKey;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "温度，0-2")
    private BigDecimal temperature;
}
