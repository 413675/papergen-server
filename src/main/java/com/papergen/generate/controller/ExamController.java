package com.papergen.generate.controller;

import com.papergen.generate.domain.model.R;
import com.papergen.generate.domain.params.ExamAnswerSaveParam;
import com.papergen.generate.service.PaperExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 答卷操作：保存作答、交卷、退回进行中
 */
@Tag(name = "答卷", description = "答题保存、交卷与退回")
@RestController
@RequestMapping("/v1/exams")
public class ExamController {

    @Autowired
    private PaperExamService paperExamService;

    @Operation(summary = "保存作答", description = "仅进行中的答卷可保存，按试卷题目覆盖作答")
    @PutMapping("/{examRecordId}/answers")
    public R<Void> saveAnswers(@PathVariable("examRecordId") Long examRecordId,
                               @RequestBody ExamAnswerSaveParam param) {
        paperExamService.saveAnswers(examRecordId, param);
        return R.ok();
    }

    @Operation(summary = "交卷", description = "客观题自动判分，错题写入错题库，主观题待批改")
    @PostMapping("/{examRecordId}/submit")
    public R<Void> submit(@PathVariable("examRecordId") Long examRecordId) {
        paperExamService.submit(examRecordId);
        return R.ok();
    }

    @Operation(summary = "退回进行中", description = "保留作答，清除判分结果")
    @PostMapping("/{examRecordId}/reopen")
    public R<Void> reopen(@PathVariable("examRecordId") Long examRecordId) {
        paperExamService.reopen(examRecordId);
        return R.ok();
    }
}
