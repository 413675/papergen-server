package com.papergen.generate.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papergen.generate.constant.ExamConstant;
import com.papergen.generate.constant.HttpStatus;
import com.papergen.generate.constant.PaperConstant;
import com.papergen.generate.constant.QuestionConstant;
import com.papergen.generate.context.PapergenContext;
import com.papergen.generate.domain.entity.ExamAnswer;
import com.papergen.generate.domain.entity.ExamRecord;
import com.papergen.generate.domain.entity.Paper;
import com.papergen.generate.domain.entity.PaperPart;
import com.papergen.generate.domain.entity.PaperQuestion;
import com.papergen.generate.domain.entity.Question;
import com.papergen.generate.domain.entity.QuestionBlank;
import com.papergen.generate.domain.entity.QuestionChoice;
import com.papergen.generate.domain.entity.QuestionComprehensive;
import com.papergen.generate.domain.entity.QuestionJudge;
import com.papergen.generate.domain.entity.QuestionShortAnswer;
import com.papergen.generate.domain.entity.WrongQuestion;
import com.papergen.generate.domain.params.ExamAnswerSaveParam;
import com.papergen.generate.domain.params.PaperReassembleParam;
import com.papergen.generate.domain.vo.ExamDetailVO;
import com.papergen.generate.domain.vo.ExamPartVO;
import com.papergen.generate.domain.vo.PaperListItemVO;
import com.papergen.generate.domain.vo.PaperReassembleVO;
import com.papergen.generate.domain.vo.QuestionVO;
import com.papergen.generate.domain.vo.WrongQuestionVO;
import com.papergen.generate.exception.BusinessException;
import com.papergen.generate.service.ExamAnswerService;
import com.papergen.generate.service.ExamRecordService;
import com.papergen.generate.service.PaperExamService;
import com.papergen.generate.service.PaperPartService;
import com.papergen.generate.service.PaperQuestionService;
import com.papergen.generate.service.PaperService;
import com.papergen.generate.service.QuestionBlankService;
import com.papergen.generate.service.QuestionChoiceService;
import com.papergen.generate.service.QuestionComprehensiveService;
import com.papergen.generate.service.QuestionJudgeService;
import com.papergen.generate.service.QuestionService;
import com.papergen.generate.service.QuestionShortAnswerService;
import com.papergen.generate.service.WrongQuestionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 试卷管理：列表、答题详情、判分交卷、错题收集与退回进行中。
 */
@Service
public class PaperExamServiceImpl implements PaperExamService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperPartService paperPartService;

    @Autowired
    private PaperQuestionService paperQuestionService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionChoiceService questionChoiceService;

    @Autowired
    private QuestionBlankService questionBlankService;

    @Autowired
    private QuestionJudgeService questionJudgeService;

    @Autowired
    private QuestionShortAnswerService questionShortAnswerService;

    @Autowired
    private QuestionComprehensiveService questionComprehensiveService;

    @Autowired
    private ExamRecordService examRecordService;

    @Autowired
    private ExamAnswerService examAnswerService;

    @Autowired
    private WrongQuestionService wrongQuestionService;

    @Override
    public List<PaperListItemVO> listMyPapers() {
        Long userId = currentUserId();
        List<Paper> papers = paperService.lambdaQuery()
                .eq(Paper::getOwnerId, userId)
                .orderByDesc(Paper::getUpdateTime)
                .list();
        if (papers.isEmpty()) {
            return List.of();
        }

        List<Long> paperIds = papers.stream().map(Paper::getPaperId).toList();
        List<ExamRecord> records = examRecordService.lambdaQuery()
                .eq(ExamRecord::getOwnerId, userId)
                .in(ExamRecord::getPaperId, paperIds)
                .orderByDesc(ExamRecord::getExamRecordId)
                .list();
        Map<Long, ExamRecord> latestRecords = new HashMap<>();
        for (ExamRecord record : records) {
            latestRecords.putIfAbsent(record.getPaperId(), record);
        }

        List<PaperListItemVO> result = new ArrayList<>(papers.size());
        for (Paper paper : papers) {
            ExamRecord record = latestRecords.get(paper.getPaperId());
            PaperListItemVO vo = new PaperListItemVO();
            vo.setPaperId(paper.getPaperId());
            vo.setTitle(paper.getTitle());
            vo.setSubject(paper.getSubject());
            vo.setDescription(paper.getDescription());
            vo.setQuestionCount(paper.getQuestionCount());
            vo.setTotalScore(paper.getTotalScore());
            vo.setUpdateTime(paper.getUpdateTime());
            if (record == null) {
                vo.setStatus(ExamConstant.STATUS_NOT_STARTED);
            } else {
                vo.setStatus(record.getStatus());
                vo.setExamRecordId(record.getExamRecordId());
                vo.setStartedAt(record.getStartedAt());
                vo.setSubmittedAt(record.getSubmittedAt());
                if (ExamConstant.STATUS_SUBMITTED.equals(record.getStatus())) {
                    vo.setScore(record.getScore());
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamDetailVO getPracticeDetail(Long paperId) {
        Long userId = currentUserId();
        Paper paper = requirePaper(paperId, userId);
        ExamRecord record = latestRecord(paperId, userId);
        if (record == null) {
            record = createRecord(paper, userId);
        }
        return buildDetail(paper, record, false);
    }

    @Override
    public ExamDetailVO getResultDetail(Long paperId) {
        Long userId = currentUserId();
        Paper paper = requirePaper(paperId, userId);
        ExamRecord record = latestRecord(paperId, userId);
        if (record == null) {
            throw new BusinessException("试卷尚未作答");
        }
        if (!ExamConstant.STATUS_SUBMITTED.equals(record.getStatus())) {
            throw new BusinessException("试卷尚未提交，请先完成答题");
        }
        return buildDetail(paper, record, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswers(Long examRecordId, ExamAnswerSaveParam param) {
        Long userId = currentUserId();
        ExamRecord record = requireRecord(examRecordId, userId);
        requireDoing(record);
        if (param == null || param.getAnswers() == null || param.getAnswers().isEmpty()) {
            return;
        }

        Map<Long, PaperQuestion> paperQuestions = paperQuestionService.lambdaQuery()
                .eq(PaperQuestion::getPaperId, record.getPaperId())
                .list()
                .stream()
                .collect(Collectors.toMap(PaperQuestion::getPaperQuestionId, Function.identity()));
        Map<Long, ExamAnswer> existing = examAnswerService.lambdaQuery()
                .eq(ExamAnswer::getExamRecordId, examRecordId)
                .list()
                .stream()
                .collect(Collectors.toMap(ExamAnswer::getPaperQuestionId, Function.identity()));

        for (ExamAnswerSaveParam.AnswerItem item : param.getAnswers()) {
            if (item == null || item.getPaperQuestionId() == null) {
                throw new BusinessException("答题数据不完整");
            }
            PaperQuestion paperQuestion = paperQuestions.get(item.getPaperQuestionId());
            if (paperQuestion == null) {
                throw new BusinessException("题目不属于当前试卷");
            }
            Object userAnswer = item.getUserAnswer() == null
                    ? null
                    : objectMapper.convertValue(item.getUserAnswer(), Object.class);
            ExamAnswer answer = existing.get(item.getPaperQuestionId());
            if (answer == null) {
                answer = new ExamAnswer();
                answer.setExamRecordId(examRecordId);
                answer.setPaperQuestionId(paperQuestion.getPaperQuestionId());
                answer.setQuestionId(paperQuestion.getQuestionId());
                answer.setUserAnswer(userAnswer);
                answer.setIsCorrect(ExamConstant.RESULT_PENDING);
                answer.setScore(BigDecimal.ZERO);
                examAnswerService.save(answer);
                existing.put(answer.getPaperQuestionId(), answer);
            } else {
                answer.setUserAnswer(userAnswer);
                examAnswerService.updateById(answer);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long examRecordId) {
        Long userId = currentUserId();
        ExamRecord record = requireRecord(examRecordId, userId);
        requireDoing(record);
        Paper paper = requirePaper(record.getPaperId(), userId);

        List<PaperQuestion> paperQuestions = paperQuestionService.lambdaQuery()
                .eq(PaperQuestion::getPaperId, paper.getPaperId())
                .orderByAsc(PaperQuestion::getSortOrder)
                .list();
        List<Long> questionIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).distinct().toList();
        Map<Long, Question> questions = questionIds.isEmpty()
                ? Map.of()
                : questionService.listByIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
        Map<Long, QuestionChoice> choices = loadChoices(questionIds);
        Map<Long, QuestionBlank> blanks = loadBlanks(questionIds);
        Map<Long, QuestionJudge> judges = loadJudges(questionIds);
        Map<Long, ExamAnswer> answers = examAnswerService.lambdaQuery()
                .eq(ExamAnswer::getExamRecordId, examRecordId)
                .list()
                .stream()
                .collect(Collectors.toMap(ExamAnswer::getPaperQuestionId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        Date now = new Date();
        for (PaperQuestion paperQuestion : paperQuestions) {
            Question question = questions.get(paperQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            ExamAnswer examAnswer = answers.get(paperQuestion.getPaperQuestionId());
            if (examAnswer == null) {
                examAnswer = new ExamAnswer();
                examAnswer.setExamRecordId(examRecordId);
                examAnswer.setPaperQuestionId(paperQuestion.getPaperQuestionId());
                examAnswer.setQuestionId(paperQuestion.getQuestionId());
            }
            GradeResult grade = grade(question, paperQuestion, examAnswer.getUserAnswer(), choices, blanks, judges);
            examAnswer.setIsCorrect(grade.isCorrect());
            examAnswer.setScore(grade.score());
            if (examAnswer.getExamAnswerId() == null) {
                examAnswerService.save(examAnswer);
            } else {
                examAnswerService.updateById(examAnswer);
            }
            total = total.add(grade.score());
            if (grade.isCorrect() == ExamConstant.RESULT_WRONG) {
                recordWrongQuestion(userId, paper.getPaperId(), question.getQuestionId(), now);
            }
        }

        record.setScore(total);
        record.setTotalScore(paper.getTotalScore() == null ? BigDecimal.ZERO : paper.getTotalScore());
        record.setStatus(ExamConstant.STATUS_SUBMITTED);
        record.setSubmittedAt(now);
        examRecordService.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopen(Long examRecordId) {
        Long userId = currentUserId();
        ExamRecord record = requireRecord(examRecordId, userId);
        if (!ExamConstant.STATUS_SUBMITTED.equals(record.getStatus())) {
            throw new BusinessException("答卷尚未提交，无需退回");
        }

        examRecordService.lambdaUpdate()
                .eq(ExamRecord::getExamRecordId, examRecordId)
                .set(ExamRecord::getStatus, ExamConstant.STATUS_DOING)
                .set(ExamRecord::getScore, BigDecimal.ZERO)
                .set(ExamRecord::getSubmittedAt, null)
                .update();
        // 保留用户作答，仅清除判分结果
        examAnswerService.lambdaUpdate()
                .eq(ExamAnswer::getExamRecordId, examRecordId)
                .set(ExamAnswer::getIsCorrect, ExamConstant.RESULT_PENDING)
                .set(ExamAnswer::getScore, BigDecimal.ZERO)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaper(Long paperId) {
        Long userId = currentUserId();
        requirePaper(paperId, userId);

        List<PaperQuestion> paperQuestions = paperQuestionService.lambdaQuery()
                .eq(PaperQuestion::getPaperId, paperId)
                .list();
        List<Long> questionIds = paperQuestions.stream()
                .map(PaperQuestion::getQuestionId)
                .distinct()
                .toList();

        // 答题记录先删答案再删答卷
        List<ExamRecord> records = examRecordService.lambdaQuery()
                .eq(ExamRecord::getPaperId, paperId)
                .list();
        if (!records.isEmpty()) {
            List<Long> recordIds = records.stream().map(ExamRecord::getExamRecordId).toList();
            examAnswerService.lambdaUpdate()
                    .in(ExamAnswer::getExamRecordId, recordIds)
                    .remove();
            examRecordService.removeByIds(recordIds);
        }

        // 该试卷产生的错题一并清理
        wrongQuestionService.lambdaUpdate()
                .eq(WrongQuestion::getPaperId, paperId)
                .eq(WrongQuestion::getOwnerId, userId)
                .remove();

        paperQuestionService.lambdaUpdate().eq(PaperQuestion::getPaperId, paperId).remove();
        paperPartService.lambdaUpdate().eq(PaperPart::getPaperId, paperId).remove();
        paperService.removeById(paperId);

        // 题目可能被多份试卷共享（如错题重组卷），只删除已无试卷引用的题目
        Set<Long> stillReferenced = questionIds.isEmpty()
                ? Set.of()
                : paperQuestionService.lambdaQuery()
                        .in(PaperQuestion::getQuestionId, questionIds)
                        .list()
                        .stream()
                        .map(PaperQuestion::getQuestionId)
                        .collect(Collectors.toSet());
        List<Long> deletableQuestionIds = questionIds.stream()
                .filter(id -> !stillReferenced.contains(id))
                .toList();
        if (!deletableQuestionIds.isEmpty()) {
            questionChoiceService.lambdaUpdate().in(QuestionChoice::getQuestionId, deletableQuestionIds).remove();
            questionBlankService.lambdaUpdate().in(QuestionBlank::getQuestionId, deletableQuestionIds).remove();
            questionJudgeService.lambdaUpdate().in(QuestionJudge::getQuestionId, deletableQuestionIds).remove();
            questionShortAnswerService.lambdaUpdate().in(QuestionShortAnswer::getQuestionId, deletableQuestionIds).remove();
            questionComprehensiveService.lambdaUpdate().in(QuestionComprehensive::getQuestionId, deletableQuestionIds).remove();
            questionService.removeByIds(deletableQuestionIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePapers(List<Long> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            throw new BusinessException("请选择要删除的试卷");
        }
        // 逐个校验归属并删除，任一失败整体回滚
        for (Long paperId : paperIds.stream().filter(Objects::nonNull).distinct().toList()) {
            deletePaper(paperId);
        }
    }

    @Override
    public List<WrongQuestionVO> listWrongQuestions() {
        Long userId = currentUserId();
        List<WrongQuestion> wrongQuestions = wrongQuestionService.lambdaQuery()
                .eq(WrongQuestion::getOwnerId, userId)
                .orderByDesc(WrongQuestion::getLastWrongTime)
                .list();
        if (wrongQuestions.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = wrongQuestions.stream()
                .map(WrongQuestion::getQuestionId)
                .distinct()
                .toList();
        Map<Long, Question> questions = questionService.listByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
        Map<Long, QuestionChoice> choices = loadChoices(questionIds);
        Map<Long, QuestionBlank> blanks = loadBlanks(questionIds);
        Map<Long, QuestionJudge> judges = loadJudges(questionIds);
        Map<Long, QuestionShortAnswer> shortAnswers = loadShortAnswers(questionIds);
        Map<Long, QuestionComprehensive> comprehensives = loadComprehensives(questionIds);

        Set<Long> paperIds = wrongQuestions.stream()
                .map(WrongQuestion::getPaperId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Paper> papers = paperIds.isEmpty()
                ? Map.of()
                : paperService.listByIds(paperIds).stream()
                        .collect(Collectors.toMap(Paper::getPaperId, Function.identity()));

        List<WrongQuestionVO> result = new ArrayList<>(wrongQuestions.size());
        for (WrongQuestion wrongQuestion : wrongQuestions) {
            Question question = questions.get(wrongQuestion.getQuestionId());
            if (question == null) {
                continue;
            }
            QuestionVO questionVO = buildQuestionVO(question, null, null, true,
                    choices, blanks, judges, shortAnswers, comprehensives);
            WrongQuestionVO vo = new WrongQuestionVO();
            BeanUtils.copyProperties(questionVO, vo);
            vo.setWrongQuestionId(wrongQuestion.getWrongQuestionId());
            vo.setPaperId(wrongQuestion.getPaperId());
            Paper paper = wrongQuestion.getPaperId() == null ? null : papers.get(wrongQuestion.getPaperId());
            vo.setPaperTitle(paper == null ? null : paper.getTitle());
            vo.setWrongCount(wrongQuestion.getWrongCount());
            vo.setLastWrongTime(wrongQuestion.getLastWrongTime());
            vo.setMastered(wrongQuestion.getMastered());
            vo.setRemark(wrongQuestion.getRemark());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWrongQuestion(Long wrongQuestionId) {
        Long userId = currentUserId();
        WrongQuestion wrongQuestion = wrongQuestionService.getById(wrongQuestionId);
        if (wrongQuestion == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "错题不存在");
        }
        if (!Objects.equals(wrongQuestion.getOwnerId(), userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权操作该错题");
        }
        wrongQuestionService.removeById(wrongQuestionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWrongQuestions(List<Long> wrongQuestionIds) {
        if (wrongQuestionIds == null || wrongQuestionIds.isEmpty()) {
            throw new BusinessException("请选择要删除的错题");
        }
        Long userId = currentUserId();
        List<Long> targets = wrongQuestionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // 只删除属于当前用户的记录
        List<Long> ownedIds = wrongQuestionService.lambdaQuery()
                .eq(WrongQuestion::getOwnerId, userId)
                .in(WrongQuestion::getWrongQuestionId, targets)
                .list()
                .stream()
                .map(WrongQuestion::getWrongQuestionId)
                .toList();
        if (!ownedIds.isEmpty()) {
            wrongQuestionService.removeByIds(ownedIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperReassembleVO reassembleFromWrongQuestions(PaperReassembleParam param) {
        Long userId = currentUserId();
        if (param == null || !StringUtils.hasText(param.getSubject())) {
            throw new BusinessException("请先选择科目");
        }
        String subject = param.getSubject().trim();

        // 取当前用户全部错题（已按最近错误时间倒序）
        List<WrongQuestion> wrongQuestions = wrongQuestionService.lambdaQuery()
                .eq(WrongQuestion::getOwnerId, userId)
                .orderByDesc(WrongQuestion::getLastWrongTime)
                .list();
        if (wrongQuestions.isEmpty()) {
            throw new BusinessException("错题本暂无题目，无法重组试卷");
        }
        List<Long> questionIds = wrongQuestions.stream()
                .map(WrongQuestion::getQuestionId)
                .distinct()
                .toList();
        Map<Long, Question> questionMap = questionService.listByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));

        // 只允许使用同一科目的错题
        List<Question> matched = wrongQuestions.stream()
                .map(wrongQuestion -> questionMap.get(wrongQuestion.getQuestionId()))
                .filter(Objects::nonNull)
                .filter(question -> question.getSubject() != null && subject.equals(question.getSubject().trim()))
                .toList();
        if (matched.isEmpty()) {
            throw new BusinessException("科目「" + subject + "」下暂无错题");
        }

        int limit = param.getQuestionCount() == null
                ? matched.size()
                : Math.min(param.getQuestionCount(), matched.size());
        if (limit <= 0) {
            throw new BusinessException("题目数量至少为 1");
        }
        if (param.getDurationMinutes() != null
                && (param.getDurationMinutes() < 1 || param.getDurationMinutes() > 600)) {
            throw new BusinessException("答题时长需在 1~600 分钟之间");
        }
        List<Question> selected = matched.subList(0, limit);

        String title = StringUtils.hasText(param.getTitle())
                ? param.getTitle().trim()
                : subject + "·错题重组卷";
        if (title.length() > 200) {
            throw new BusinessException("试卷名称不能超过 200 个字符");
        }

        Paper paper = new Paper();
        paper.setOwnerId(userId);
        paper.setTitle(title);
        paper.setDescription("由错题本按科目自动重组");
        paper.setSubject(subject);
        paper.setStatus(PaperConstant.STATUS_DRAFT);
        paper.setTotalScore(BigDecimal.ZERO);
        paper.setQuestionCount(0);
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("source", "wrong_question");
        if (param.getDurationMinutes() != null) {
            settings.put("durationMinutes", param.getDurationMinutes());
        }
        paper.setSettings(settings);
        paperService.save(paper);

        PaperPart part = new PaperPart();
        part.setPaperId(paper.getPaperId());
        part.setTitle("错题重组（" + subject + "）");
        part.setInstruction("以下题目来自你的错题本，按最近错误时间排序");
        part.setSortOrder(0);
        paperPartService.save(part);

        BigDecimal totalScore = BigDecimal.ZERO;
        int sortOrder = 0;
        for (Question question : selected) {
            BigDecimal score = question.getDefaultScore() == null ? BigDecimal.ZERO : question.getDefaultScore();
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(paper.getPaperId());
            paperQuestion.setPaperPartId(part.getPaperPartId());
            paperQuestion.setQuestionId(question.getQuestionId());
            paperQuestion.setSortOrder(sortOrder++);
            paperQuestion.setScore(score);
            paperQuestionService.save(paperQuestion);
            totalScore = totalScore.add(score);
        }

        paper.setQuestionCount(sortOrder);
        paper.setTotalScore(totalScore);
        paperService.updateById(paper);

        PaperReassembleVO vo = new PaperReassembleVO();
        vo.setPaperId(paper.getPaperId());
        vo.setTitle(paper.getTitle());
        vo.setSubject(subject);
        vo.setQuestionCount(sortOrder);
        vo.setTotalScore(totalScore);
        return vo;
    }

    private ExamRecord createRecord(Paper paper, Long userId) {
        ExamRecord record = new ExamRecord();
        record.setPaperId(paper.getPaperId());
        record.setOwnerId(userId);
        record.setStatus(ExamConstant.STATUS_DOING);
        record.setScore(BigDecimal.ZERO);
        record.setTotalScore(paper.getTotalScore() == null ? BigDecimal.ZERO : paper.getTotalScore());
        record.setStartedAt(new Date());
        examRecordService.save(record);
        return record;
    }

    private ExamDetailVO buildDetail(Paper paper, ExamRecord record, boolean includeAnswer) {
        List<PaperPart> parts = paperPartService.lambdaQuery()
                .eq(PaperPart::getPaperId, paper.getPaperId())
                .orderByAsc(PaperPart::getSortOrder)
                .list();
        List<PaperQuestion> paperQuestions = paperQuestionService.lambdaQuery()
                .eq(PaperQuestion::getPaperId, paper.getPaperId())
                .orderByAsc(PaperQuestion::getSortOrder)
                .list();
        List<Long> questionIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).distinct().toList();

        Map<Long, Question> questions = questionIds.isEmpty()
                ? Map.of()
                : questionService.listByIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
        Map<Long, QuestionChoice> choices = loadChoices(questionIds);
        Map<Long, QuestionBlank> blanks = loadBlanks(questionIds);
        Map<Long, QuestionJudge> judges = loadJudges(questionIds);
        Map<Long, QuestionShortAnswer> shortAnswers = loadShortAnswers(questionIds);
        Map<Long, QuestionComprehensive> comprehensives = loadComprehensives(questionIds);
        Map<Long, ExamAnswer> examAnswers = examAnswerService.lambdaQuery()
                .eq(ExamAnswer::getExamRecordId, record.getExamRecordId())
                .list()
                .stream()
                .collect(Collectors.toMap(ExamAnswer::getPaperQuestionId, Function.identity()));

        Map<Long, List<PaperQuestion>> questionsByPart = paperQuestions.stream()
                .filter(item -> item.getPaperPartId() != null)
                .collect(Collectors.groupingBy(PaperQuestion::getPaperPartId, LinkedHashMap::new, Collectors.toList()));

        List<ExamPartVO> partVOs = new ArrayList<>(parts.size());
        for (PaperPart part : parts) {
            ExamPartVO partVO = new ExamPartVO();
            partVO.setPaperPartId(part.getPaperPartId());
            partVO.setTitle(part.getTitle());
            partVO.setInstruction(part.getInstruction());
            List<QuestionVO> questionVOs = new ArrayList<>();
            for (PaperQuestion paperQuestion : questionsByPart.getOrDefault(part.getPaperPartId(), List.of())) {
                Question question = questions.get(paperQuestion.getQuestionId());
                if (question == null) {
                    continue;
                }
                questionVOs.add(buildQuestionVO(question, paperQuestion,
                        examAnswers.get(paperQuestion.getPaperQuestionId()), includeAnswer,
                        choices, blanks, judges, shortAnswers, comprehensives));
            }
            partVO.setQuestions(questionVOs);
            partVOs.add(partVO);
        }

        ExamDetailVO detail = new ExamDetailVO();
        detail.setExamRecordId(record.getExamRecordId());
        detail.setPaperId(paper.getPaperId());
        detail.setTitle(paper.getTitle());
        detail.setDescription(paper.getDescription());
        detail.setSubject(paper.getSubject());
        detail.setStatus(record.getStatus());
        detail.setTotalScore(record.getTotalScore());
        detail.setScore(record.getScore());
        detail.setStartedAt(record.getStartedAt());
        detail.setSubmittedAt(record.getSubmittedAt());
        detail.setParts(partVOs);
        return detail;
    }

    private QuestionVO buildQuestionVO(Question question, PaperQuestion paperQuestion, ExamAnswer examAnswer,
                                       boolean includeAnswer, Map<Long, QuestionChoice> choices,
                                       Map<Long, QuestionBlank> blanks, Map<Long, QuestionJudge> judges,
                                       Map<Long, QuestionShortAnswer> shortAnswers,
                                       Map<Long, QuestionComprehensive> comprehensives) {
        QuestionVO vo = new QuestionVO();
        vo.setQuestionId(question.getQuestionId());
        vo.setType(question.getType());
        vo.setSubject(question.getSubject());
        vo.setStem(extractText(question.getStem()));
        if (paperQuestion == null) {
            // 错题本等场景没有试卷题目关系，直接使用题目默认分值
            vo.setScore(question.getDefaultScore());
        } else {
            vo.setPaperQuestionId(paperQuestion.getPaperQuestionId());
            vo.setSortOrder(paperQuestion.getSortOrder());
            vo.setScore(paperQuestion.getScore() != null ? paperQuestion.getScore() : question.getDefaultScore());
        }

        switch (question.getType()) {
            case QuestionConstant.TYPE_CHOICE -> {
                QuestionChoice choice = choices.get(question.getQuestionId());
                if (choice != null) {
                    vo.setChoiceMode(choice.getChoiceMode());
                    vo.setOptions(toMapList(choice.getOptions()));
                    if (includeAnswer) {
                        vo.setAnswer(choice.getAnswer());
                    }
                }
            }
            case QuestionConstant.TYPE_BLANK -> {
                QuestionBlank blank = blanks.get(question.getQuestionId());
                if (blank != null) {
                    vo.setBlankCount(blank.getBlankCount());
                    if (includeAnswer) {
                        vo.setAnswer(blank.getAnswer());
                    }
                }
            }
            case QuestionConstant.TYPE_JUDGE -> {
                QuestionJudge judge = judges.get(question.getQuestionId());
                if (includeAnswer && judge != null) {
                    vo.setAnswer(judge.getAnswer());
                }
            }
            case QuestionConstant.TYPE_SHORT_ANSWER -> {
                QuestionShortAnswer shortAnswer = shortAnswers.get(question.getQuestionId());
                if (includeAnswer && shortAnswer != null) {
                    vo.setReferenceAnswer(shortAnswer.getReferenceAnswer());
                    vo.setScoringPoints(shortAnswer.getScoringPoints());
                }
            }
            case QuestionConstant.TYPE_COMPREHENSIVE -> {
                QuestionComprehensive comprehensive = comprehensives.get(question.getQuestionId());
                if (comprehensive != null) {
                    vo.setMaterial(extractText(comprehensive.getMaterial()));
                    vo.setSubQuestions(buildSubQuestions(comprehensive.getSubQuestions(), includeAnswer));
                    if (includeAnswer) {
                        vo.setReferenceAnswer(comprehensive.getReferenceAnswer());
                    }
                }
            }
            default -> {
                // 未知题型只返回公共信息
            }
        }

        if (examAnswer != null) {
            Object userAnswer = examAnswer.getUserAnswer();
            // 未作答时列默认值为 '{}'，统一归一化为 null
            if (userAnswer instanceof Map<?, ?> emptyCheck && emptyCheck.isEmpty()) {
                userAnswer = null;
            }
            vo.setUserAnswer(userAnswer);
            if (includeAnswer) {
                vo.setIsCorrect(examAnswer.getIsCorrect());
                vo.setEarnedScore(examAnswer.getScore());
            }
        }
        if (includeAnswer) {
            vo.setAnalysis(question.getAnalysis());
        }
        return vo;
    }

    private GradeResult grade(Question question, PaperQuestion paperQuestion, Object userAnswer,
                              Map<Long, QuestionChoice> choices, Map<Long, QuestionBlank> blanks,
                              Map<Long, QuestionJudge> judges) {
        BigDecimal fullScore = paperQuestion.getScore() == null ? BigDecimal.ZERO : paperQuestion.getScore();
        switch (question.getType()) {
            case QuestionConstant.TYPE_CHOICE -> {
                QuestionChoice choice = choices.get(question.getQuestionId());
                List<String> expected = choice == null ? List.of() : toStringList(choice.getAnswer());
                boolean correct = !expected.isEmpty() && sameChoice(expected, toStringList(userAnswer));
                return correct
                        ? new GradeResult(ExamConstant.RESULT_CORRECT, fullScore)
                        : new GradeResult(ExamConstant.RESULT_WRONG, BigDecimal.ZERO);
            }
            case QuestionConstant.TYPE_BLANK -> {
                QuestionBlank blank = blanks.get(question.getQuestionId());
                List<String> expected = blank == null ? List.of() : toStringList(blank.getAnswer());
                if (expected.isEmpty()) {
                    return new GradeResult(ExamConstant.RESULT_WRONG, BigDecimal.ZERO);
                }
                List<String> actual = toStringList(userAnswer);
                int correctCount = 0;
                for (int i = 0; i < expected.size(); i++) {
                    String userValue = i < actual.size() ? actual.get(i) : "";
                    if (sameText(expected.get(i), userValue)) {
                        correctCount++;
                    }
                }
                BigDecimal perBlank = fullScore.divide(BigDecimal.valueOf(expected.size()), 2, RoundingMode.HALF_UP);
                BigDecimal earned = perBlank.multiply(BigDecimal.valueOf(correctCount)).setScale(2, RoundingMode.HALF_UP);
                boolean allCorrect = correctCount == expected.size();
                return new GradeResult(allCorrect ? ExamConstant.RESULT_CORRECT : ExamConstant.RESULT_WRONG,
                        allCorrect ? fullScore : earned);
            }
            case QuestionConstant.TYPE_JUDGE -> {
                QuestionJudge judge = judges.get(question.getQuestionId());
                Boolean expected = judge == null ? null : judge.getAnswer();
                Boolean actual = toBoolean(userAnswer);
                boolean correct = expected != null && expected.equals(actual);
                return correct
                        ? new GradeResult(ExamConstant.RESULT_CORRECT, fullScore)
                        : new GradeResult(ExamConstant.RESULT_WRONG, BigDecimal.ZERO);
            }
            default -> {
                // 简答题、大题需人工批改
                return new GradeResult(ExamConstant.RESULT_PENDING, BigDecimal.ZERO);
            }
        }
    }

    private void recordWrongQuestion(Long userId, Long paperId, Long questionId, Date now) {
        WrongQuestion wrongQuestion = wrongQuestionService.lambdaQuery()
                .eq(WrongQuestion::getOwnerId, userId)
                .eq(WrongQuestion::getQuestionId, questionId)
                .one();
        if (wrongQuestion == null) {
            wrongQuestion = new WrongQuestion();
            wrongQuestion.setOwnerId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setPaperId(paperId);
            wrongQuestion.setWrongCount(1);
            wrongQuestion.setLastWrongTime(now);
            wrongQuestion.setMastered(0);
            wrongQuestionService.save(wrongQuestion);
        } else {
            wrongQuestion.setPaperId(paperId);
            wrongQuestion.setWrongCount((wrongQuestion.getWrongCount() == null ? 0 : wrongQuestion.getWrongCount()) + 1);
            wrongQuestion.setLastWrongTime(now);
            wrongQuestion.setMastered(0);
            wrongQuestionService.updateById(wrongQuestion);
        }
    }

    private Map<Long, QuestionChoice> loadChoices(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionChoiceService.lambdaQuery()
                .in(QuestionChoice::getQuestionId, questionIds)
                .list()
                .stream()
                .collect(Collectors.toMap(QuestionChoice::getQuestionId, Function.identity()));
    }

    private Map<Long, QuestionBlank> loadBlanks(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionBlankService.lambdaQuery()
                .in(QuestionBlank::getQuestionId, questionIds)
                .list()
                .stream()
                .collect(Collectors.toMap(QuestionBlank::getQuestionId, Function.identity()));
    }

    private Map<Long, QuestionJudge> loadJudges(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionJudgeService.lambdaQuery()
                .in(QuestionJudge::getQuestionId, questionIds)
                .list()
                .stream()
                .collect(Collectors.toMap(QuestionJudge::getQuestionId, Function.identity()));
    }

    private Map<Long, QuestionShortAnswer> loadShortAnswers(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionShortAnswerService.lambdaQuery()
                .in(QuestionShortAnswer::getQuestionId, questionIds)
                .list()
                .stream()
                .collect(Collectors.toMap(QuestionShortAnswer::getQuestionId, Function.identity()));
    }

    private Map<Long, QuestionComprehensive> loadComprehensives(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionComprehensiveService.lambdaQuery()
                .in(QuestionComprehensive::getQuestionId, questionIds)
                .list()
                .stream()
                .collect(Collectors.toMap(QuestionComprehensive::getQuestionId, Function.identity()));
    }

    private Paper requirePaper(Long paperId, Long userId) {
        Paper paper = paperService.getById(paperId);
        if (paper == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "试卷不存在");
        }
        if (!Objects.equals(paper.getOwnerId(), userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权访问该试卷");
        }
        return paper;
    }

    private ExamRecord requireRecord(Long examRecordId, Long userId) {
        ExamRecord record = examRecordService.getById(examRecordId);
        if (record == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "答卷不存在");
        }
        if (!Objects.equals(record.getOwnerId(), userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权操作该答卷");
        }
        return record;
    }

    private void requireDoing(ExamRecord record) {
        if (!ExamConstant.STATUS_DOING.equals(record.getStatus())) {
            throw new BusinessException("答卷已提交，请先退回进行中");
        }
    }

    private ExamRecord latestRecord(Long paperId, Long userId) {
        return examRecordService.lambdaQuery()
                .eq(ExamRecord::getPaperId, paperId)
                .eq(ExamRecord::getOwnerId, userId)
                .orderByDesc(ExamRecord::getExamRecordId)
                .last("LIMIT 1")
                .one();
    }

    private Long currentUserId() {
        Long userId = PapergenContext.getUserId();
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }

    /** 题干/材料 JSON 取 text 字段 */
    private String extractText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Object text = map.get("text");
            return text == null ? null : String.valueOf(text);
        }
        return String.valueOf(value);
    }

    private List<Map<String, Object>> toMapList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>(collection.size());
        for (Object item : collection) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                map.forEach((key, val) -> copy.put(String.valueOf(key), val));
                result.add(copy);
            }
        }
        return result;
    }

    /** 综合题小题：进行中隐藏小题答案 */
    private List<Map<String, Object>> buildSubQuestions(Object value, boolean includeAnswer) {
        List<Map<String, Object>> subs = toMapList(value);
        if (subs == null) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>(subs.size());
        for (Map<String, Object> sub : subs) {
            Map<String, Object> copy = new LinkedHashMap<>();
            copy.put("stem", sub.get("stem"));
            if (sub.get("score") != null) {
                copy.put("score", sub.get("score"));
            }
            if (includeAnswer && sub.get("answer") != null) {
                copy.put("answer", sub.get("answer"));
            }
            result.add(copy);
        }
        return result;
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        Collection<?> collection = value instanceof Collection<?> items ? items : List.of(value);
        return collection.stream()
                .filter(Objects::nonNull)
                .map(item -> String.valueOf(item).trim())
                .toList();
    }

    private boolean sameChoice(List<String> expected, List<String> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        List<String> expectedKeys = expected.stream().map(this::normalizeKey).sorted().toList();
        List<String> actualKeys = actual.stream().map(this::normalizeKey).sorted().toList();
        return expectedKeys.equals(actualKeys);
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private boolean sameText(String expected, String actual) {
        return expected != null && actual != null && expected.trim().equalsIgnoreCase(actual.trim());
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    /** 单题判分结果 */
    private record GradeResult(int isCorrect, BigDecimal score) {
    }
}
