package com.papergen.generate.domain.params;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 试卷导入文件结构（version = 1）。
 * paper 描述试卷信息，parts 按大题分组，题目按 type 决定使用哪些字段。
 */
@Schema(description = "试卷导入文件")
@Data
public class PaperImportParam {

    @Schema(description = "格式版本，当前仅支持 1")
    private Integer version;

    @Schema(description = "试卷信息")
    private PaperMeta paper;

    @Schema(description = "大题列表，数组顺序即试卷顺序")
    private List<Part> parts;

    @Schema(description = "试卷信息")
    @Data
    public static class PaperMeta {

        @Schema(description = "试卷标题，必填")
        private String title;

        @Schema(description = "学科，如“操作系统（Linux）”")
        private String subject;

        @Schema(description = "试卷说明")
        private String description;

        @Schema(description = "考试时长（分钟），存入 paper.settings")
        private Integer durationMinutes;

        @Schema(description = "其他组卷配置，原样存入 paper.settings")
        private Map<String, Object> settings;
    }

    @Schema(description = "试卷大题")
    @Data
    public static class Part {

        @Schema(description = "大题标题，必填，如“一、单项选择题”")
        private String title;

        @Schema(description = "答题说明")
        private String instruction;

        @Schema(description = "题目列表")
        private List<QuestionItem> questions;
    }

    @Schema(description = "题目")
    @Data
    public static class QuestionItem {

        @Schema(description = "题型：choice/blank/judge/short_answer/comprehensive")
        private String type;

        @Schema(description = "题干，纯文本")
        private String stem;

        @Schema(description = "答案解析")
        private String analysis;

        @Schema(description = "本题分值，必填")
        private BigDecimal score;

        @Schema(description = "学科，不填时继承试卷学科")
        private String subject;

        @Schema(description = "选择题：single-单选 | multiple-多选，默认 single")
        private String choiceMode;

        @Schema(description = "选择题选项：[{key, content}]")
        private List<Option> options;

        @Schema(description = "答案：选择题/填空题为字符串数组，判断题为布尔值")
        private JsonNode answer;

        @Schema(description = "填空题空格数量，不填时等于答案个数")
        private Integer blankCount;

        @Schema(description = "简答题参考答案")
        private String referenceAnswer;

        @Schema(description = "评分要点，字符串数组")
        private List<String> scoringPoints;

        @Schema(description = "综合题材料/背景")
        private String material;

        @Schema(description = "综合题小题：[{stem, answer, score}]")
        private List<SubQuestion> subQuestions;
    }

    @Schema(description = "选择题选项")
    @Data
    public static class Option {

        @Schema(description = "选项标识，如 A")
        private String key;

        @Schema(description = "选项内容")
        private String content;
    }

    @Schema(description = "综合题小题")
    @Data
    public static class SubQuestion {

        @Schema(description = "小题题干，必填")
        private String stem;

        @Schema(description = "参考答案")
        private String answer;

        @Schema(description = "小题分值")
        private BigDecimal score;
    }
}
