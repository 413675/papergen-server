package com.papergen.generate.domain.params;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 错题重组试卷参数
 */
@Schema(description = "错题重组试卷参数")
@Data
public class PaperReassembleParam {

    @Schema(description = "科目，必填，只能使用同一科目的错题")
    private String subject;

    @Schema(description = "试卷名称，为空时自动生成")
    private String title;

    @Schema(description = "题目数量，默认使用该科目全部错题")
    private Integer questionCount;

    @Schema(description = "答题时长（分钟）")
    private Integer durationMinutes;
}
