package com.papergen.generate.domain.params;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 保存答题参数，按试卷题目批量覆盖作答。
 */
@Schema(description = "保存答题参数")
@Data
public class ExamAnswerSaveParam {

    @Schema(description = "作答列表")
    private List<AnswerItem> answers;

    @Schema(description = "单题作答")
    @Data
    public static class AnswerItem {

        @Schema(description = "试卷题目ID")
        private Long paperQuestionId;

        @Schema(description = "用户作答：选择题/填空题为字符串数组，判断题为布尔值，简答题为字符串，大题为字符串数组")
        private JsonNode userAnswer;
    }
}
