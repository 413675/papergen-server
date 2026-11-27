package com.papergen.generate.domain.params;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量删除参数
 */
@Schema(description = "批量删除参数")
@Data
public class BatchDeleteParam {

    @Schema(description = "待删除的记录ID列表")
    private List<Long> ids;
}
