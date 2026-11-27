package com.papergen.generate.service;

import com.github.yulichang.base.MPJBaseService;
import com.papergen.generate.domain.entity.AiModelConfig;

/**
 * AI 模型配置
 */
public interface AiModelConfigService extends MPJBaseService<AiModelConfig> {

    /**
     * 查询用户的模型配置
     */
    AiModelConfig getByOwner(Long ownerId);
}
