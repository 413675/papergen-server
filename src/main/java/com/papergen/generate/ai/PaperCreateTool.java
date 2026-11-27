package com.papergen.generate.ai;

import com.papergen.generate.domain.vo.PaperImportVO;
import com.papergen.generate.service.PaperImportService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 试卷生成工具：供 AI 工作流以工具调用方式保存试卷
 */
@Component
public class PaperCreateTool {

    @Autowired
    private PaperImportService paperImportService;

    @Tool(name = "createPaper", description = "把符合格式要求的试卷 JSON 保存为一份可答题的试卷，返回试卷ID、标题、题数与总分")
    public PaperImportVO createPaper(@ToolParam(description = "完整试卷 JSON 字符串") String paperJson) {
        return paperImportService.importPaper(paperJson, "ai-generated-paper.json");
    }
}
