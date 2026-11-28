package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 请求进度：前端在等待期间轮询，展示是否正在因模型繁忙而重试
 */
@Data
@Schema(description = "AI 请求进度")
public class AiProgressVO {

    @Schema(description = "状态：IDLE/GENERATING/RETRYING")
    private String status;

    @Schema(description = "当前第几次重试")
    private int retry;

    @Schema(description = "最多重试次数")
    private int maxRetries;

    @Schema(description = "本次重试前等待的秒数")
    private long waitSeconds;
}
