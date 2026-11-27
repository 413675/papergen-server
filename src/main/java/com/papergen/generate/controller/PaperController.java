package com.papergen.generate.controller;

import com.papergen.generate.domain.model.R;
import com.papergen.generate.domain.params.BatchDeleteParam;
import com.papergen.generate.domain.params.PaperReassembleParam;
import com.papergen.generate.domain.vo.ExamDetailVO;
import com.papergen.generate.domain.vo.PaperListItemVO;
import com.papergen.generate.domain.vo.PaperReassembleVO;
import com.papergen.generate.service.PaperExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 试卷管理：列表、进行中答题详情、已完成解析详情
 */
@Tag(name = "试卷管理", description = "试卷列表与答题/解析详情")
@RestController
@RequestMapping("/v1/papers")
public class PaperController {

    @Autowired
    private PaperExamService paperExamService;

    @Operation(summary = "我的试卷列表", description = "返回当前用户的试卷，status 为 not_started / doing / submitted")
    @GetMapping
    public R<List<PaperListItemVO>> list() {
        return R.ok(paperExamService.listMyPapers());
    }

    @Operation(summary = "进行中答题详情", description = "只返回题干与用户已保存的作答，不含答案与解析")
    @GetMapping("/{paperId}/practice")
    public R<ExamDetailVO> practice(@PathVariable("paperId") Long paperId) {
        return R.ok(paperExamService.getPracticeDetail(paperId));
    }

    @Operation(summary = "已完成解析详情", description = "返回答案、解析、用户作答与判分结果")
    @GetMapping("/{paperId}/result")
    public R<ExamDetailVO> result(@PathVariable("paperId") Long paperId) {
        return R.ok(paperExamService.getResultDetail(paperId));
    }

    @Operation(summary = "删除试卷", description = "同时删除答题记录、错题记录与导入的题目")
    @DeleteMapping("/{paperId}")
    public R<Void> delete(@PathVariable("paperId") Long paperId) {
        paperExamService.deletePaper(paperId);
        return R.ok();
    }

    @Operation(summary = "批量删除试卷", description = "按 ID 列表批量删除，任一份不存在或无权限时整体回滚")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody BatchDeleteParam param) {
        paperExamService.deletePapers(param.getIds());
        return R.ok();
    }

    @Operation(summary = "错题重组试卷", description = "按科目挑选错题生成一份新试卷，只使用同一科目的题目")
    @PostMapping("/reassemble")
    public R<PaperReassembleVO> reassemble(@RequestBody PaperReassembleParam param) {
        return R.ok(paperExamService.reassembleFromWrongQuestions(param));
    }
}
