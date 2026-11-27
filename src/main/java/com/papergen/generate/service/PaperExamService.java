package com.papergen.generate.service;

import com.papergen.generate.domain.params.ExamAnswerSaveParam;
import com.papergen.generate.domain.params.PaperReassembleParam;
import com.papergen.generate.domain.vo.ExamDetailVO;
import com.papergen.generate.domain.vo.PaperListItemVO;
import com.papergen.generate.domain.vo.PaperReassembleVO;
import com.papergen.generate.domain.vo.WrongQuestionVO;

import java.util.List;

/**
 * 试卷管理：试卷列表、答题详情、交卷与退回，以及错题本。
 */
public interface PaperExamService {

    /** 当前用户的试卷列表，含作答状态 */
    List<PaperListItemVO> listMyPapers();

    /** 进行中详情：只含题干，不含答案与解析，不存在答卷时自动创建 */
    ExamDetailVO getPracticeDetail(Long paperId);

    /** 已提交详情：含答案、解析、用户作答与判分结果 */
    ExamDetailVO getResultDetail(Long paperId);

    /** 保存答题（仅进行中） */
    void saveAnswers(Long examRecordId, ExamAnswerSaveParam param);

    /** 交卷：自动判分、记录总分、错题入错题库 */
    void submit(Long examRecordId);

    /** 退回进行中：保留作答，清除判分结果 */
    void reopen(Long examRecordId);

    /** 删除试卷及其答题记录、错题记录 */
    void deletePaper(Long paperId);

    /** 批量删除试卷 */
    void deletePapers(List<Long> paperIds);

    /** 当前用户的错题列表，含答案与解析 */
    List<WrongQuestionVO> listWrongQuestions();

    /** 删除一条错题记录 */
    void deleteWrongQuestion(Long wrongQuestionId);

    /** 批量删除错题记录 */
    void deleteWrongQuestions(List<Long> wrongQuestionIds);

    /** 按科目用错题重组一份新试卷 */
    PaperReassembleVO reassembleFromWrongQuestions(PaperReassembleParam param);
}
