package com.papergen.generate.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papergen.generate.constant.HttpStatus;
import com.papergen.generate.constant.PaperConstant;
import com.papergen.generate.constant.QuestionConstant;
import com.papergen.generate.constant.SourceConstant;
import com.papergen.generate.constant.TaskConstant;
import com.papergen.generate.context.PapergenContext;
import com.papergen.generate.domain.entity.GenerationTask;
import com.papergen.generate.domain.entity.Paper;
import com.papergen.generate.domain.entity.PaperPart;
import com.papergen.generate.domain.entity.PaperQuestion;
import com.papergen.generate.domain.entity.Question;
import com.papergen.generate.domain.entity.QuestionBlank;
import com.papergen.generate.domain.entity.QuestionChoice;
import com.papergen.generate.domain.entity.QuestionComprehensive;
import com.papergen.generate.domain.entity.QuestionJudge;
import com.papergen.generate.domain.entity.QuestionShortAnswer;
import com.papergen.generate.domain.entity.SourceDocument;
import com.papergen.generate.domain.params.PaperImportParam;
import com.papergen.generate.domain.vo.PaperImportVO;
import com.papergen.generate.exception.BusinessException;
import com.papergen.generate.service.GenerationTaskService;
import com.papergen.generate.service.PaperImportService;
import com.papergen.generate.service.PaperPartService;
import com.papergen.generate.service.PaperQuestionService;
import com.papergen.generate.service.PaperService;
import com.papergen.generate.service.QuestionBlankService;
import com.papergen.generate.service.QuestionChoiceService;
import com.papergen.generate.service.QuestionComprehensiveService;
import com.papergen.generate.service.QuestionJudgeService;
import com.papergen.generate.service.QuestionService;
import com.papergen.generate.service.QuestionShortAnswerService;
import com.papergen.generate.service.SourceDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 试卷导入：解析 JSON → 落库为草稿试卷（源文件、任务、大题、题目、题目子表、试卷题目关系）
 */
@Service
public class PaperImportServiceImpl implements PaperImportService {

    /** 当前支持的导入格式版本 */
    private static final int IMPORT_VERSION = 1;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final int MAX_QUESTION_COUNT = 500;

    /** 与 question.default_score numeric(6,2) 上限一致 */
    private static final BigDecimal MAX_SCORE = new BigDecimal("9999.99");

    private static final Set<String> QUESTION_TYPES = Set.of(
            QuestionConstant.TYPE_CHOICE,
            QuestionConstant.TYPE_BLANK,
            QuestionConstant.TYPE_JUDGE,
            QuestionConstant.TYPE_SHORT_ANSWER,
            QuestionConstant.TYPE_COMPREHENSIVE);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SourceDocumentService sourceDocumentService;

    @Autowired
    private GenerationTaskService generationTaskService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperImportVO importPaper(MultipartFile file) {
        Long userId = PapergenContext.getUserId();
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        // 先读文件并完成全部校验，避免无效数据落库
        String rawJson = readFile(file);
        PaperImportParam param = parse(rawJson);
        validate(param);

        SourceDocument document = saveSourceDocument(userId, file, rawJson);
        GenerationTask task = saveTask(userId, document.getSourceDocumentId(), file.getOriginalFilename());

        Paper paper = savePaper(userId, param.getPaper());

        int sortOrder = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        for (int i = 0; i < param.getParts().size(); i++) {
            PaperImportParam.Part partParam = param.getParts().get(i);
            PaperPart part = savePart(paper.getPaperId(), partParam, i);
            for (PaperImportParam.QuestionItem item : partParam.getQuestions()) {
                Question question = saveQuestion(userId, document.getSourceDocumentId(), paper.getSubject(), item);
                saveQuestionDetail(question.getQuestionId(), item);
                savePaperQuestion(paper.getPaperId(), part.getPaperPartId(), question.getQuestionId(), sortOrder, item.getScore());
                sortOrder++;
                totalScore = totalScore.add(item.getScore());
            }
        }

        paper.setQuestionCount(sortOrder);
        paper.setTotalScore(totalScore);
        paperService.updateById(paper);

        task.setPaperId(paper.getPaperId());
        task.setStatus(TaskConstant.STATUS_SUCCEEDED);
        task.setProgress(TaskConstant.PROGRESS_DONE);
        task.setFinishedAt(new Date());
        task.setResult(Map.of(
                "paperId", paper.getPaperId(),
                "questionCount", sortOrder,
                "totalScore", totalScore));
        generationTaskService.updateById(task);

        PaperImportVO vo = new PaperImportVO();
        vo.setTaskId(task.getGenerationTaskId());
        vo.setPaperId(paper.getPaperId());
        vo.setTitle(paper.getTitle());
        vo.setSubject(paper.getSubject());
        vo.setQuestionCount(sortOrder);
        vo.setTotalScore(totalScore);
        return vo;
    }

    private String readFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 JSON 文件");
        }
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName) || !fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new BusinessException("仅支持 .json 格式的导入文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("导入文件不能超过 10MB");
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            // 去掉 Windows 编辑器可能写入的 BOM
            return content.startsWith("\uFEFF") ? content.substring(1) : content;
        } catch (IOException e) {
            throw new BusinessException("读取导入文件失败");
        }
    }

    private PaperImportParam parse(String rawJson) {
        try {
            PaperImportParam param = objectMapper.readValue(rawJson, PaperImportParam.class);
            if (param == null) {
                throw new BusinessException("文件内容为空");
            }
            return param;
        } catch (JsonProcessingException e) {
            throw new BusinessException("文件不是合法的 JSON：" + e.getOriginalMessage());
        }
    }

    private void validate(PaperImportParam param) {
        if (!Integer.valueOf(IMPORT_VERSION).equals(param.getVersion())) {
            throw new BusinessException("不支持的格式版本，当前仅支持 version = " + IMPORT_VERSION);
        }
        PaperImportParam.PaperMeta meta = param.getPaper();
        if (meta == null) {
            throw new BusinessException("缺少 paper 节点");
        }
        requireText(meta.getTitle(), "paper.title", 200);
        if (StringUtils.hasText(meta.getSubject()) && meta.getSubject().length() > 64) {
            throw new BusinessException("paper.subject 长度不能超过 64 个字符");
        }

        List<PaperImportParam.Part> parts = param.getParts();
        if (parts == null || parts.isEmpty()) {
            throw new BusinessException("parts 不能为空，至少需要一个试卷大题");
        }
        int questionCount = 0;
        for (int i = 0; i < parts.size(); i++) {
            PaperImportParam.Part part = parts.get(i);
            String partLabel = "第 " + (i + 1) + " 个大题";
            if (part == null) {
                throw new BusinessException(partLabel + " 不能为空");
            }
            requireText(part.getTitle(), partLabel + " 的 title", 100);
            if (part.getQuestions() == null || part.getQuestions().isEmpty()) {
                throw new BusinessException(partLabel + " 至少需要一道题目");
            }
            for (int j = 0; j < part.getQuestions().size(); j++) {
                validateQuestion(part.getQuestions().get(j), partLabel + " 第 " + (j + 1) + " 题");
            }
            questionCount += part.getQuestions().size();
        }
        if (questionCount > MAX_QUESTION_COUNT) {
            throw new BusinessException("单次导入题目数量不能超过 " + MAX_QUESTION_COUNT + " 道");
        }
    }

    private void validateQuestion(PaperImportParam.QuestionItem item, String label) {
        if (item == null) {
            throw new BusinessException(label + " 不能为空");
        }
        if (!QUESTION_TYPES.contains(item.getType())) {
            throw new BusinessException(label + " 的 type 必须是 choice、blank、judge、short_answer、comprehensive 之一");
        }
        requireText(item.getStem(), label + " 的 stem", 0);
        if (item.getScore() == null || item.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(label + " 的 score 必须大于 0");
        }
        if (item.getScore().compareTo(MAX_SCORE) > 0) {
            throw new BusinessException(label + " 的 score 不能超过 " + MAX_SCORE.toPlainString());
        }

        switch (item.getType()) {
            case QuestionConstant.TYPE_CHOICE -> validateChoice(item, label);
            case QuestionConstant.TYPE_BLANK -> validateBlank(item, label);
            case QuestionConstant.TYPE_JUDGE -> validateJudge(item, label);
            case QuestionConstant.TYPE_COMPREHENSIVE -> validateComprehensive(item, label);
            default -> {
                // 简答题除公共字段外无额外必填项
            }
        }
    }

    private void validateChoice(PaperImportParam.QuestionItem item, String label) {
        String mode = StringUtils.hasText(item.getChoiceMode())
                ? item.getChoiceMode()
                : QuestionConstant.CHOICE_MODE_SINGLE;
        if (!QuestionConstant.CHOICE_MODE_SINGLE.equals(mode)
                && !QuestionConstant.CHOICE_MODE_MULTIPLE.equals(mode)) {
            throw new BusinessException(label + " 的 choiceMode 只能是 single 或 multiple");
        }
        List<PaperImportParam.Option> options = item.getOptions();
        if (options == null || options.size() < 2) {
            throw new BusinessException(label + " 的 options 至少需要 2 个选项");
        }
        Set<String> keys = new LinkedHashSet<>();
        for (PaperImportParam.Option option : options) {
            if (option == null || !StringUtils.hasText(option.getKey())) {
                throw new BusinessException(label + " 存在空的选项标识");
            }
            String key = option.getKey().trim();
            if (!keys.add(key)) {
                throw new BusinessException(label + " 的选项标识重复：" + key);
            }
            if (!StringUtils.hasText(option.getContent())) {
                throw new BusinessException(label + " 的选项 " + key + " 内容不能为空");
            }
        }

        List<String> answers = validateStringAnswers(item.getAnswer(), label);
        if (QuestionConstant.CHOICE_MODE_SINGLE.equals(mode) && answers.size() != 1) {
            throw new BusinessException(label + " 是单选题，answer 必须有且只有一个选项标识");
        }
        if (QuestionConstant.CHOICE_MODE_MULTIPLE.equals(mode) && answers.size() < 2) {
            throw new BusinessException(label + " 是多选题，answer 至少需要两个选项标识");
        }
        for (String answer : answers) {
            if (!keys.contains(answer)) {
                throw new BusinessException(label + " 的答案 " + answer + " 不在选项中");
            }
        }
    }

    private void validateBlank(PaperImportParam.QuestionItem item, String label) {
        List<String> answers = validateStringAnswers(item.getAnswer(), label);
        for (String answer : answers) {
            if (!StringUtils.hasText(answer)) {
                throw new BusinessException(label + " 的 answer 存在空答案");
            }
        }
        if (item.getBlankCount() != null && item.getBlankCount() != answers.size()) {
            throw new BusinessException(label + " 的 blankCount 必须等于答案个数 " + answers.size());
        }
    }

    private void validateJudge(PaperImportParam.QuestionItem item, String label) {
        JsonNode answer = item.getAnswer();
        if (answer == null || !answer.isBoolean()) {
            throw new BusinessException(label + " 是判断题，answer 必须是 true 或 false");
        }
    }

    private void validateComprehensive(PaperImportParam.QuestionItem item, String label) {
        List<PaperImportParam.SubQuestion> subQuestions = item.getSubQuestions();
        if (subQuestions == null || subQuestions.isEmpty()) {
            throw new BusinessException(label + " 是综合题，subQuestions 至少需要一道小题");
        }
        for (int i = 0; i < subQuestions.size(); i++) {
            PaperImportParam.SubQuestion sub = subQuestions.get(i);
            if (sub == null) {
                throw new BusinessException(label + " 第 " + (i + 1) + " 个小题不能为空");
            }
            requireText(sub.getStem(), label + " 第 " + (i + 1) + " 个小题的 stem", 0);
            if (sub.getScore() != null && sub.getScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(label + " 第 " + (i + 1) + " 个小题的 score 不能为负数");
            }
        }
    }

    /** 校验答案是非空字符串数组，返回去除首尾空格后的答案 */
    private List<String> validateStringAnswers(JsonNode node, String label) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new BusinessException(label + " 的 answer 必须是非空字符串数组");
        }
        List<String> answers = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new BusinessException(label + " 的 answer 必须是非空字符串数组");
            }
            answers.add(item.asText().trim());
        }
        return answers;
    }

    private void requireText(String value, String field, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(field + " 不能为空");
        }
        if (maxLength > 0 && value.length() > maxLength) {
            throw new BusinessException(field + " 长度不能超过 " + maxLength + " 个字符");
        }
    }

    private SourceDocument saveSourceDocument(Long userId, MultipartFile file, String rawJson) {
        SourceDocument document = new SourceDocument();
        document.setOwnerId(userId);
        document.setFileName(file.getOriginalFilename());
        document.setStorage(SourceConstant.STORAGE_INLINE);
        document.setMimeType("application/json");
        document.setFileSize(file.getSize());
        document.setContentText(rawJson);
        document.setStatus(SourceConstant.STATUS_PARSED);
        sourceDocumentService.save(document);
        return document;
    }

    private GenerationTask saveTask(Long userId, Long documentId, String fileName) {
        GenerationTask task = new GenerationTask();
        task.setOwnerId(userId);
        task.setDocumentId(documentId);
        task.setTaskType(TaskConstant.TYPE_IMPORT);
        task.setStatus(TaskConstant.STATUS_PROCESSING);
        task.setProgress(0);
        task.setParams(Map.of("fileName", fileName, "version", IMPORT_VERSION));
        task.setStartedAt(new Date());
        generationTaskService.save(task);
        return task;
    }

    private Paper savePaper(Long userId, PaperImportParam.PaperMeta meta) {
        Paper paper = new Paper();
        paper.setOwnerId(userId);
        paper.setTitle(meta.getTitle().trim());
        paper.setDescription(meta.getDescription());
        paper.setSubject(StringUtils.hasText(meta.getSubject()) ? meta.getSubject().trim() : null);
        paper.setStatus(PaperConstant.STATUS_DRAFT);
        paper.setTotalScore(BigDecimal.ZERO);
        paper.setQuestionCount(0);
        paper.setSettings(buildSettings(meta));
        paperService.save(paper);
        return paper;
    }

    private Map<String, Object> buildSettings(PaperImportParam.PaperMeta meta) {
        Map<String, Object> settings = new LinkedHashMap<>();
        if (meta.getSettings() != null) {
            settings.putAll(meta.getSettings());
        }
        if (meta.getDurationMinutes() != null) {
            settings.put("durationMinutes", meta.getDurationMinutes());
        }
        return settings;
    }

    private PaperPart savePart(Long paperId, PaperImportParam.Part partParam, int sortOrder) {
        PaperPart part = new PaperPart();
        part.setPaperId(paperId);
        part.setTitle(partParam.getTitle().trim());
        part.setInstruction(partParam.getInstruction());
        part.setSortOrder(sortOrder);
        paperPartService.save(part);
        return part;
    }

    private Question saveQuestion(Long userId, Long documentId, String paperSubject,
                                  PaperImportParam.QuestionItem item) {
        Question question = new Question();
        question.setOwnerId(userId);
        question.setSourceDocumentId(documentId);
        question.setSubject(StringUtils.hasText(item.getSubject())
                ? item.getSubject().trim()
                : paperSubject);
        question.setType(item.getType());
        question.setStem(Collections.singletonMap("text", item.getStem()));
        question.setStemText(item.getStem());
        question.setAnalysis(item.getAnalysis());
        question.setDefaultScore(item.getScore());
        questionService.save(question);
        return question;
    }

    private void saveQuestionDetail(Long questionId, PaperImportParam.QuestionItem item) {
        switch (item.getType()) {
            case QuestionConstant.TYPE_CHOICE -> saveChoice(questionId, item);
            case QuestionConstant.TYPE_BLANK -> saveBlank(questionId, item);
            case QuestionConstant.TYPE_JUDGE -> saveJudge(questionId, item);
            case QuestionConstant.TYPE_SHORT_ANSWER -> saveShortAnswer(questionId, item);
            case QuestionConstant.TYPE_COMPREHENSIVE -> saveComprehensive(questionId, item);
            default -> throw new BusinessException("不支持的题型：" + item.getType());
        }
    }

    private void saveChoice(Long questionId, PaperImportParam.QuestionItem item) {
        List<Map<String, String>> options = new ArrayList<>();
        for (PaperImportParam.Option option : item.getOptions()) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("key", option.getKey().trim());
            map.put("content", option.getContent().trim());
            options.add(map);
        }
        QuestionChoice choice = new QuestionChoice();
        choice.setQuestionId(questionId);
        choice.setChoiceMode(StringUtils.hasText(item.getChoiceMode())
                ? item.getChoiceMode()
                : QuestionConstant.CHOICE_MODE_SINGLE);
        choice.setOptions(options);
        choice.setAnswer(toAnswerStrings(item.getAnswer()));
        questionChoiceService.save(choice);
    }

    private void saveBlank(Long questionId, PaperImportParam.QuestionItem item) {
        List<String> answers = toAnswerStrings(item.getAnswer());
        QuestionBlank blank = new QuestionBlank();
        blank.setQuestionId(questionId);
        blank.setBlankCount(item.getBlankCount() != null ? item.getBlankCount() : answers.size());
        blank.setAnswer(answers);
        questionBlankService.save(blank);
    }

    private void saveJudge(Long questionId, PaperImportParam.QuestionItem item) {
        QuestionJudge judge = new QuestionJudge();
        judge.setQuestionId(questionId);
        judge.setAnswer(item.getAnswer().asBoolean());
        questionJudgeService.save(judge);
    }

    private void saveShortAnswer(Long questionId, PaperImportParam.QuestionItem item) {
        QuestionShortAnswer shortAnswer = new QuestionShortAnswer();
        shortAnswer.setQuestionId(questionId);
        shortAnswer.setReferenceAnswer(item.getReferenceAnswer());
        shortAnswer.setScoringPoints(item.getScoringPoints() == null ? List.of() : item.getScoringPoints());
        questionShortAnswerService.save(shortAnswer);
    }

    private void saveComprehensive(Long questionId, PaperImportParam.QuestionItem item) {
        List<Map<String, Object>> subQuestions = new ArrayList<>();
        for (PaperImportParam.SubQuestion sub : item.getSubQuestions()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("stem", sub.getStem());
            if (sub.getAnswer() != null) {
                map.put("answer", sub.getAnswer());
            }
            if (sub.getScore() != null) {
                map.put("score", sub.getScore());
            }
            subQuestions.add(map);
        }
        QuestionComprehensive comprehensive = new QuestionComprehensive();
        comprehensive.setQuestionId(questionId);
        comprehensive.setMaterial(StringUtils.hasText(item.getMaterial())
                ? Collections.singletonMap("text", item.getMaterial())
                : null);
        comprehensive.setSubQuestions(subQuestions);
        comprehensive.setReferenceAnswer(item.getReferenceAnswer());
        questionComprehensiveService.save(comprehensive);
    }

    private void savePaperQuestion(Long paperId, Long paperPartId, Long questionId, int sortOrder, BigDecimal score) {
        PaperQuestion paperQuestion = new PaperQuestion();
        paperQuestion.setPaperId(paperId);
        paperQuestion.setPaperPartId(paperPartId);
        paperQuestion.setQuestionId(questionId);
        paperQuestion.setSortOrder(sortOrder);
        paperQuestion.setScore(score);
        paperQuestionService.save(paperQuestion);
    }

    /** 校验通过后的答案数组转换 */
    private List<String> toAnswerStrings(JsonNode node) {
        List<String> answers = new ArrayList<>();
        node.forEach(item -> answers.add(item.asText().trim()));
        return answers;
    }
}
