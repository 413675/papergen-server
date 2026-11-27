package com.papergen.generate.service.impl;

import com.github.yulichang.base.MPJBaseServiceImpl;
import com.papergen.generate.domain.entity.AiModelConfig;
import com.papergen.generate.mapper.AiModelConfigMapper;
import com.papergen.generate.service.AiModelConfigService;
import org.springframework.stereotype.Service;

/**
 * AI 模型配置实现
 */
@Service
public class AiModelConfigServiceImpl extends MPJBaseServiceImpl<AiModelConfigMapper, AiModelConfig>
        implements AiModelConfigService {

    @Override
    public AiModelConfig getByOwner(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(AiModelConfig::getOwnerId, ownerId)
                .last("limit 1")
                .one();
    }
}
