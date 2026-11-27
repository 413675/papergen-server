package com.papergen.generate;

import com.papergen.generate.constant.PaperConstant;
import com.papergen.generate.constant.UserConstant;
import com.papergen.generate.domain.entity.Paper;
import com.papergen.generate.domain.entity.SysUser;
import com.papergen.generate.mapper.PaperMapper;
import com.papergen.generate.mapper.PaperPartMapper;
import com.papergen.generate.mapper.PaperQuestionMapper;
import com.papergen.generate.mapper.QuestionMapper;
import com.papergen.generate.service.SysUserService;
import com.papergen.generate.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 试卷导入接口集成测试（事务结束后回滚，不污染数据库）
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaperImportIntegrationTest {

    private static final String VALID_JSON = """
            {
              "version": 1,
              "paper": {
                "title": "导入接口测试卷",
                "subject": "操作系统",
                "description": "测试用",
                "durationMinutes": 120
              },
              "parts": [
                {
                  "title": "一、基础题",
                  "instruction": "共 4 题",
                  "questions": [
                    {
                      "type": "choice",
                      "choiceMode": "single",
                      "stem": "Linux 中一切皆文件，以下哪个目录存放设备文件？",
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
                      "type": "choice",
                      "choiceMode": "multiple",
                      "stem": "以下属于进程状态的有（　）",
                      "options": [
                        {"key": "A", "content": "运行"},
                        {"key": "B", "content": "就绪"},
                        {"key": "C", "content": "阻塞"},
                        {"key": "D", "content": "编译"}
                      ],
                      "answer": ["A", "B", "C"],
                      "score": 4
                    },
                    {
                      "type": "blank",
                      "stem": "进程由程序、____和____三部分组成。",
                      "blankCount": 2,
                      "answer": ["数据", "进程控制块"],
                      "score": 4
                    },
                    {
                      "type": "judge",
                      "stem": "线程是资源分配的基本单位。",
                      "answer": false,
                      "score": 2
                    }
                  ]
                },
                {
                  "title": "二、主观题",
                  "instruction": "写出要点",
                  "questions": [
                    {
                      "type": "short_answer",
                      "stem": "简述死锁产生的四个必要条件。",
                      "referenceAnswer": "互斥、请求并保持、不可剥夺、循环等待。",
                      "scoringPoints": ["互斥", "请求并保持", "不可剥夺", "循环等待"],
                      "score": 10
                    },
                    {
                      "type": "comprehensive",
                      "stem": "阅读材料并回答问题。",
                      "material": "某系统采用分页存储管理，页面大小为 4KB。",
                      "subQuestions": [
                        {"stem": "（1）逻辑地址由哪两部分组成？", "answer": "页号和页内偏移", "score": 5},
                        {"stem": "（2）页内偏移占多少位？", "answer": "12 位", "score": 5}
                      ],
                      "referenceAnswer": "（1）页号和页内偏移；（2）12 位。",
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
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private PaperPartMapper paperPartMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    private String token;

    private Long testerUserId;

    private long paperCountBefore;

    private long paperPartCountBefore;

    private long questionCountBefore;

    private long paperQuestionCountBefore;

    @BeforeEach
    void setUp() {
        paperCountBefore = paperMapper.selectCount(null);
        paperPartCountBefore = paperPartMapper.selectCount(null);
        questionCountBefore = questionMapper.selectCount(null);
        paperQuestionCountBefore = paperQuestionMapper.selectCount(null);

        SysUser user = new SysUser();
        user.setUsername("import_tester");
        user.setPasswordHash("not-a-real-hash");
        user.setDisplayName("导入测试");
        user.setRole(UserConstant.DEFAULT_ROLE);
        user.setStatus(UserConstant.STATUS_ENABLED);
        sysUserService.save(user);
        testerUserId = user.getSysUserId();
        token = jwtUtil.generateToken(user);
    }

    @Test
    void importCreatesDraftPaperWithQuestions() throws Exception {
        mockMvc.perform(multipart("/v1/generation/tasks/import")
                        .file(jsonFile(VALID_JSON))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.questionCount").value(6))
                .andExpect(jsonPath("$.data.totalScore").value(33));

        assertEquals(paperCountBefore + 1, paperMapper.selectCount(null));
        assertEquals(paperPartCountBefore + 2, paperPartMapper.selectCount(null));
        assertEquals(questionCountBefore + 6, questionMapper.selectCount(null));
        assertEquals(paperQuestionCountBefore + 6, paperQuestionMapper.selectCount(null));

        Paper paper = paperMapper.selectList(null).stream()
                .filter(item -> testerUserId.equals(item.getOwnerId()))
                .findFirst()
                .orElseThrow();
        assertEquals(PaperConstant.STATUS_DRAFT, paper.getStatus());
        assertEquals(6, paper.getQuestionCount());
        assertEquals(0, paper.getTotalScore().compareTo(new java.math.BigDecimal("33")));
    }

    @Test
    void importRejectsChoiceAnswerNotInOptions() throws Exception {
        String invalid = VALID_JSON.replace("\"answer\": [\"B\"]", "\"answer\": [\"E\"]");

        mockMvc.perform(multipart("/v1/generation/tasks/import")
                        .file(jsonFile(invalid))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg", containsString("不在选项中")));

        assertEquals(paperCountBefore, paperMapper.selectCount(null));
        assertEquals(questionCountBefore, questionMapper.selectCount(null));
    }

    @Test
    void importRequiresLogin() throws Exception {
        mockMvc.perform(multipart("/v1/generation/tasks/import").file(jsonFile(VALID_JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    private MockMultipartFile jsonFile(String content) {
        return new MockMultipartFile(
                "file",
                "paper.json",
                "application/json",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
