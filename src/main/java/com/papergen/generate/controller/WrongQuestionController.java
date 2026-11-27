package com.papergen.generate.controller;

import com.papergen.generate.domain.model.R;
import com.papergen.generate.domain.params.BatchDeleteParam;
import com.papergen.generate.domain.vo.WrongQuestionVO;
import com.papergen.generate.service.PaperExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 错题本：错题列表与删除
 */
@Tag(name = "错题本", description = "错题列表与删除")
@RestController
@RequestMapping("/v1/wrong-questions")
public class WrongQuestionController {

    @Autowired
    private PaperExamService paperExamService;

    @Operation(summary = "我的错题列表", description = "返回错题及正确答案与解析，按最近错误时间倒序")
    @GetMapping
    public R<List<WrongQuestionVO>> list() {
        return R.ok(paperExamService.listWrongQuestions());
    }

    @Operation(summary = "删除错题", description = "删除一条错题记录")
    @DeleteMapping("/{wrongQuestionId}")
    public R<Void> delete(@PathVariable("wrongQuestionId") Long wrongQuestionId) {
        paperExamService.deleteWrongQuestion(wrongQuestionId);
        return R.ok();
    }

    @Operation(summary = "批量删除错题", description = "按 ID 列表批量删除当前用户的错题")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody BatchDeleteParam param) {
        paperExamService.deleteWrongQuestions(param.getIds());
        return R.ok();
    }
}
