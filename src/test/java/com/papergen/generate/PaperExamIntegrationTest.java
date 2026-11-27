package com.papergen.generate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.papergen.generate.constant.ExamConstant;
import com.papergen.generate.constant.UserConstant;
import com.papergen.generate.domain.entity.SysUser;
import com.papergen.generate.domain.entity.WrongQuestion;
import com.papergen.generate.mapper.QuestionMapper;
import com.papergen.generate.mapper.WrongQuestionMapper;
import com.papergen.generate.service.SysUserService;
import com.papergen.generate.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 试卷管理集成测试：开始答题、保存作答、交卷判分、错题入库、退回进行中。
 * 复用导入接口造数据，测试结束由事务回滚。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaperExamIntegrationTest {

    /** 1 个大题 4 道题：选择 3 分、填空 4 分、判断 2 分、简答 10 分，总分 19 */
    private static final String PAPER_JSON = """
            {
              "version": 1,
              "paper": {
                "title": "试卷管理测试卷",
                "subject": "操作系统"
              },
              "parts": [
                {
                  "title": "一、基础题",
                  "instruction": "请作答",
                  "questions": [
                    {
                      "type": "choice",
                      "choiceMode": "single",
                      "stem": "以下哪个目录存放设备文件？",
                      "options": [
                        {"key": "A", "content": "/etc"},
                        {"key": "B", "content": "/dev"},
                        {"key": "C", "content": "/bin"},
                        {"key": "D", "content": "/home"}
                      ],
                      "answer": ["B"],
                      "analysis": "/dev 存放设备文件。",
                      "score": 3
                    },
                    {
                      "type": "blank",
                      "stem": "进程由____、____和进程控制块组成。",
                      "blankCount": 2,
                      "answer": ["程序", "数据"],
                      "analysis": "进程由程序、数据和进程控制块组成。",
                      "score": 4
                    },
                    {
                      "type": "judge",
                      "stem": "线程是资源分配的基本单位。",
                      "answer": false,
                      "analysis": "进程才是资源分配的基本单位。",
                      "score": 2
                    },
                    {
                      "type": "short_answer",
                      "stem": "简述死锁产生的四个必要条件。",
                      "referenceAnswer": "互斥、请求并保持、不可剥夺、循环等待。",
                      "scoringPoints": ["互斥", "请求并保持", "不可剥夺", "循环等待"],
                      "score": 10
                    }
                  ]
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    private String authorization;

    private Long userId;

    @BeforeEach
    void setUp() {
        SysUser user = new SysUser();
        user.setUsername("exam_tester");
        user.setPasswordHash("not-a-real-hash");
        user.setDisplayName("试卷管理测试");
        user.setRole(UserConstant.DEFAULT_ROLE);
        user.setStatus(UserConstant.STATUS_ENABLED);
        sysUserService.save(user);
        userId = user.getSysUserId();
        authorization = "Bearer " + jwtUtil.generateToken(user);
    }

    @Test
    void practiceDetailHidesAnswerAndMarksPaperDoing() throws Exception {
        long paperId = importPaper();

        // 导入后尚未开始
        MvcResult listBefore = mockMvc.perform(get("/v1/papers").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].status").value(ExamConstant.STATUS_NOT_STARTED))
                .andReturn();
        assertEquals(paperId, readJson(listBefore).at("/data/0/paperId").asLong());

        JsonNode practice = practice(paperId);
        assertEquals(ExamConstant.STATUS_DOING, practice.at("/data/status").asText());
        assertTrue(practice.at("/data/examRecordId").asLong() > 0);

        JsonNode questions = practice.at("/data/parts/0/questions");
        assertEquals(4, questions.size());
        // 进行中不返回答案与解析
        assertFalse(questions.get(0).has("answer"));
        assertFalse(questions.get(0).has("analysis"));
        assertFalse(questions.get(1).has("answer"));
        assertFalse(questions.get(3).has("referenceAnswer"));

        // 开始后列表状态变为进行中
        mockMvc.perform(get("/v1/papers").header("Authorization", authorization))
                .andExpect(jsonPath("$.data[0].status").value(ExamConstant.STATUS_DOING));
    }

    @Test
    void submitGradesAnswersAndWritesWrongQuestion() throws Exception {
        long paperId = importPaper();
        JsonNode practice = practice(paperId);
        long examRecordId = practice.at("/data/examRecordId").asLong();
        String answers = buildAnswers(practice.at("/data/parts"));

        mockMvc.perform(put("/v1/exams/{examRecordId}/answers", examRecordId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/v1/exams/{examRecordId}/submit", examRecordId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        JsonNode result = result(paperId);
        assertEquals(ExamConstant.STATUS_SUBMITTED, result.at("/data/status").asText());
        // 选择 3 + 填空部分对 2 + 判断 2 + 简答待批改 0 = 7
        assertTrue(new BigDecimal("7").compareTo(result.at("/data/score").decimalValue()) == 0);

        JsonNode questions = result.at("/data/parts/0/questions");
        // 选择题：答对，返回答案与解析
        assertEquals(ExamConstant.RESULT_CORRECT, questions.get(0).at("/isCorrect").asInt());
        assertEquals("B", questions.get(0).at("/answer/0").asText());
        assertTrue(questions.get(0).has("analysis"));
        // 填空题：答对一空，给部分分但整体记为错
        assertEquals(ExamConstant.RESULT_WRONG, questions.get(1).at("/isCorrect").asInt());
        assertEquals(2.00, questions.get(1).at("/earnedScore").asDouble(), 0.001);
        // 判断题：答对
        assertEquals(ExamConstant.RESULT_CORRECT, questions.get(2).at("/isCorrect").asInt());
        // 简答题：待批改，返回参考答案
        assertEquals(ExamConstant.RESULT_PENDING, questions.get(3).at("/isCorrect").asInt());
        assertTrue(questions.get(3).has("referenceAnswer"));

        // 只有填空题进错题库
        LambdaQueryWrapper<WrongQuestion> wrongQuery = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getOwnerId, userId);
        assertEquals(1L, wrongQuestionMapper.selectCount(wrongQuery).longValue());
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectOne(wrongQuery);
        assertEquals(1, wrongQuestion.getWrongCount());
        assertEquals("blank", questionMapper.selectById(wrongQuestion.getQuestionId()).getType());

        // 列表展示已完成与得分
        MvcResult listResult = mockMvc.perform(get("/v1/papers").header("Authorization", authorization))
                .andExpect(jsonPath("$.data[0].status").value(ExamConstant.STATUS_SUBMITTED))
                .andReturn();
        JsonNode listItem = readJson(listResult).at("/data/0");
        assertTrue(new BigDecimal("7").compareTo(listItem.at("/score").decimalValue()) == 0);
    }

    @Test
    void reopenBackToDoingKeepsAnswersAndClearsResult() throws Exception {
        long paperId = importPaper();
        JsonNode practice = practice(paperId);
        long examRecordId = practice.at("/data/examRecordId").asLong();

        mockMvc.perform(put("/v1/exams/{examRecordId}/answers", examRecordId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildAnswers(practice.at("/data/parts"))))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/v1/exams/{examRecordId}/submit", examRecordId)
                        .header("Authorization", authorization))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/v1/exams/{examRecordId}/reopen", examRecordId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 退回后可以继续作答，作答内容保留
        JsonNode reopened = practice(paperId);
        assertEquals(ExamConstant.STATUS_DOING, reopened.at("/data/status").asText());
        assertEquals("错误答案", reopened.at("/data/parts/0/questions/1/userAnswer/1").asText());

        // 未提交时不能查看解析
        mockMvc.perform(get("/v1/papers/{paperId}/result", paperId).header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void wrongBookCanListAndDeleteAndPaperDeletionClearsAll() throws Exception {
        long paperId = importPaper();
        JsonNode practice = practice(paperId);
        long examRecordId = practice.at("/data/examRecordId").asLong();
        mockMvc.perform(put("/v1/exams/{examRecordId}/answers", examRecordId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildAnswers(practice.at("/data/parts"))))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/v1/exams/{examRecordId}/submit", examRecordId)
                        .header("Authorization", authorization))
                .andExpect(jsonPath("$.code").value(200));

        // 错题列表返回题目、正确答案、解析与来源试卷
        MvcResult wrongList = mockMvc.perform(get("/v1/wrong-questions")
                        .header("Authorization", authorization))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].wrongCount").value(1))
                .andExpect(jsonPath("$.data[0].paperTitle").value("试卷管理测试卷"))
                .andExpect(jsonPath("$.data[0].answer[0]").value("程序"))
                .andExpect(jsonPath("$.data[0].analysis").isNotEmpty())
                .andReturn();
        JsonNode wrongItem = readJson(wrongList).at("/data/0");
        assertEquals(paperId, wrongItem.at("/paperId").asLong());
        long wrongQuestionId = wrongItem.at("/wrongQuestionId").asLong();

        // 错题重组：只使用同科目错题，可配置名称、题量与时长
        MvcResult reassemble = mockMvc.perform(post("/v1/papers/reassemble")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"操作系统\",\"title\":\"考前错题重组卷\","
                                + "\"questionCount\":1,\"durationMinutes\":30}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("考前错题重组卷"))
                .andExpect(jsonPath("$.data.subject").value("操作系统"))
                .andExpect(jsonPath("$.data.questionCount").value(1))
                .andReturn();
        long reassembledPaperId = readJson(reassemble).at("/data/paperId").asLong();

        // 其他科目没有错题，不能重组
        mockMvc.perform(post("/v1/papers/reassemble")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"大学英语\"}"))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/v1/papers").header("Authorization", authorization))
                .andExpect(jsonPath("$.data.length()").value(2));

        // 删除重组卷后原卷仍可查看，共享的题目未被误删
        mockMvc.perform(delete("/v1/papers/{paperId}", reassembledPaperId)
                        .header("Authorization", authorization))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/v1/papers/{paperId}/result", paperId)
                        .header("Authorization", authorization))
                .andExpect(jsonPath("$.code").value(200));

        // 批量删除错题
        mockMvc.perform(delete("/v1/wrong-questions/batch")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + wrongQuestionId + "]}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/v1/wrong-questions").header("Authorization", authorization))
                .andExpect(jsonPath("$.data.length()").value(0));

        // 批量删除试卷后列表为空，相关答题记录一并清理
        mockMvc.perform(delete("/v1/papers/batch")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + paperId + "]}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/v1/papers").header("Authorization", authorization))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private long importPaper() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "paper.json", "application/json",
                PAPER_JSON.getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart("/v1/generation/tasks/import")
                        .file(file)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return readJson(result).at("/data/paperId").asLong();
    }

    private JsonNode practice(long paperId) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/papers/{paperId}/practice", paperId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return readJson(result);
    }

    private JsonNode result(long paperId) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/papers/{paperId}/result", paperId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return readJson(result);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** 按题型构造作答：填空故意答错一空，用于验证部分得分与错题入库 */
    private String buildAnswers(JsonNode parts) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode part : parts) {
            for (JsonNode question : part.at("/questions")) {
                Object answer = switch (question.at("/type").asText()) {
                    case "choice" -> List.of("B");
                    case "blank" -> List.of("程序", "错误答案");
                    case "judge" -> false;
                    case "short_answer" -> "互斥、请求并保持、不可剥夺、循环等待。";
                    default -> null;
                };
                items.add(Map.of(
                        "paperQuestionId", question.at("/paperQuestionId").asLong(),
                        "userAnswer", answer));
            }
        }
        return objectMapper.writeValueAsString(Map.of("answers", items));
    }
}
