package com.papergen.domain.model;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Schema(description = "分页查询参数")
@Data
public class PageQuery {
    /** 当前记录起始索引 */
    @Schema(description = "当前记录起始索引", required = true)
    @NotNull(message = "当前记录起始索引不能为空")
    @Min(value = 1, message = "当前页码必须大于0")
    private Integer pageNum;

    /** 每页显示记录数 */
    @Schema(description = "每页显示记录数", required = true)
    @NotNull(message = "每页显示记录数不能为空")
    @Min(value = 1, message = "每页显示记录数必须大于0")
    private Integer pageSize;
}
