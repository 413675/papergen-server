package com.papergen.generate.controller;

import com.papergen.generate.domain.model.R;
import com.papergen.generate.domain.vo.PaperImportVO;
import com.papergen.generate.service.PaperImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 生成任务：导入试卷文件
 */
@Tag(name = "生成任务", description = "试卷导入等生成任务")
@RestController
@RequestMapping("/v1/generation")
public class GenerationTaskController {

    @Autowired
    private PaperImportService paperImportService;

    @Operation(summary = "导入试卷文件", description = "上传 paper-import v1 JSON 文件，自动生成草稿试卷")
    @PostMapping(value = "/tasks/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<PaperImportVO> importPaper(@RequestPart("file") MultipartFile file) {
        return R.ok(paperImportService.importPaper(file), "导入成功");
    }
}
