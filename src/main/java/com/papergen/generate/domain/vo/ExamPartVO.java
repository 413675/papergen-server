package com.papergen.generate.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 试卷大题（部分），包含其下题目。
 */
@Schema(description = "试卷大题")
@Data
public class ExamPartVO {

    @Schema(description = "试卷大题ID")
    private Long paperPartId;

    @Schema(description = "大题标题")
    private String title;

    @Schema(description = "答题说明")
    private String instruction;

    @Schema(description = "题目列表")
    private List<QuestionVO> questions;
}
