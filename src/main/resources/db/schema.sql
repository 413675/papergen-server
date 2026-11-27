-- ============================================================
-- 试卷助手（papergen）Schema — PostgreSQL 16
--
-- 约定：
-- 1. 主键 bigint，MyBatis-Plus 雪花ID，命名 <表名>_id。
-- 2. 公共字段 create_by / create_time / update_by / update_time，AutoHandler 自动填充。
-- 3. 时间 timestamptz；jsonb 存结构多变内容。
-- 4. 字典字段在字段行尾标 key-value。
-- 5. 不建物理外键，仅保留关联 ID 字段，关联关系由应用层维护。
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 用户
CREATE TABLE sys_user (
    sys_user_id   bigint PRIMARY KEY,
    username      varchar(64)   NOT NULL,
    password_hash varchar(255)  NOT NULL,
    display_name  varchar(64),
    role          varchar(16)   NOT NULL DEFAULT 'teacher'
                  CHECK (role IN ('teacher', 'admin')),          -- teacher-教师 | admin-管理员
    status        smallint      NOT NULL DEFAULT 1,              -- 1-正常 | 0-禁用
    create_by     bigint,
    create_time   timestamptz   NOT NULL DEFAULT now(),
    update_by     bigint,
    update_time   timestamptz   NOT NULL DEFAULT now()
);
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.sys_user_id IS '主键';
COMMENT ON COLUMN sys_user.username IS '登录账号';
COMMENT ON COLUMN sys_user.password_hash IS '密码哈希';
COMMENT ON COLUMN sys_user.display_name IS '显示名称';
COMMENT ON COLUMN sys_user.role IS 'teacher-教师 | admin-管理员';
COMMENT ON COLUMN sys_user.status IS '1-正常 | 0-禁用';
COMMENT ON COLUMN sys_user.create_by IS '创建人';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_by IS '更新人';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username);

-- 源文件
CREATE TABLE source_document (
    source_document_id bigint PRIMARY KEY,
    owner_id           bigint,
    file_name          varchar(255) NOT NULL,
    object_key         varchar(512),
    storage            varchar(16) NOT NULL DEFAULT 'r2'
                       CHECK (storage IN ('r2', 'local', 'inline')), -- r2-R2 | local-本地 | inline-纯文本
    mime_type          varchar(128),
    file_size          bigint,
    page_count         int,
    content_text       text,
    status             varchar(16) NOT NULL DEFAULT 'uploaded'
                       CHECK (status IN ('uploaded', 'parsing', 'parsed', 'failed')), -- uploaded-已上传 | parsing-解析中 | parsed-已解析 | failed-失败
    error_message      text,
    create_by          bigint,
    create_time        timestamptz NOT NULL DEFAULT now(),
    update_by          bigint,
    update_time        timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE source_document IS '上传源文件表';
COMMENT ON COLUMN source_document.source_document_id IS '主键';
COMMENT ON COLUMN source_document.owner_id IS '所属用户ID';
COMMENT ON COLUMN source_document.file_name IS '文件名';
COMMENT ON COLUMN source_document.object_key IS '对象存储键';
COMMENT ON COLUMN source_document.storage IS 'r2-R2 | local-本地 | inline-纯文本';
COMMENT ON COLUMN source_document.mime_type IS 'MIME类型';
COMMENT ON COLUMN source_document.file_size IS '文件字节数';
COMMENT ON COLUMN source_document.page_count IS '页数';
COMMENT ON COLUMN source_document.content_text IS '解析后的纯文本';
COMMENT ON COLUMN source_document.status IS 'uploaded-已上传 | parsing-解析中 | parsed-已解析 | failed-失败';
COMMENT ON COLUMN source_document.error_message IS '失败原因';
COMMENT ON COLUMN source_document.create_by IS '创建人';
COMMENT ON COLUMN source_document.create_time IS '创建时间';
COMMENT ON COLUMN source_document.update_by IS '更新人';
COMMENT ON COLUMN source_document.update_time IS '更新时间';
CREATE INDEX idx_doc_owner ON source_document (owner_id, create_time DESC);

-- 题目公共表
CREATE TABLE question (
    question_id        bigint PRIMARY KEY,
    owner_id           bigint,
    source_document_id bigint NOT NULL,
    subject            varchar(64),
    type               varchar(24) NOT NULL
                       CHECK (type IN ('choice', 'blank', 'judge', 'short_answer', 'comprehensive')), -- choice-选择题 | blank-填空题 | judge-判断题 | short_answer-简答题 | comprehensive-大题
    stem               jsonb NOT NULL DEFAULT '{}'::jsonb,
    stem_text          text,
    analysis           text,
    default_score      numeric(6,2) DEFAULT 2,
    create_by          bigint,
    create_time        timestamptz NOT NULL DEFAULT now(),
    update_by          bigint,
    update_time        timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE question IS '题目公共表';
COMMENT ON COLUMN question.question_id IS '主键';
COMMENT ON COLUMN question.owner_id IS '所属用户ID';
COMMENT ON COLUMN question.source_document_id IS '来源文档ID，必填，题目只能来自文件导入';
COMMENT ON COLUMN question.subject IS '学科';
COMMENT ON COLUMN question.type IS 'choice-选择题 | blank-填空题 | judge-判断题 | short_answer-简答题 | comprehensive-大题';
COMMENT ON COLUMN question.stem IS '题干JSON';
COMMENT ON COLUMN question.stem_text IS '题干明文';
COMMENT ON COLUMN question.analysis IS '答案解析';
COMMENT ON COLUMN question.default_score IS '默认分值';
COMMENT ON COLUMN question.create_by IS '创建人';
COMMENT ON COLUMN question.create_time IS '创建时间';
COMMENT ON COLUMN question.update_by IS '更新人';
COMMENT ON COLUMN question.update_time IS '更新时间';
CREATE INDEX idx_question_owner ON question (owner_id, create_time DESC);
CREATE INDEX idx_question_type ON question (type);
CREATE INDEX idx_question_doc ON question (source_document_id);
CREATE INDEX idx_question_subject ON question (owner_id, subject);
CREATE INDEX idx_question_stem_trgm ON question USING gin (stem_text gin_trgm_ops);

-- 选择题
CREATE TABLE question_choice (
    question_choice_id bigint PRIMARY KEY,
    question_id        bigint NOT NULL,
    choice_mode        varchar(16) NOT NULL DEFAULT 'single'
                       CHECK (choice_mode IN ('single', 'multiple')), -- single-单选 | multiple-多选
    options            jsonb NOT NULL DEFAULT '[]'::jsonb,
    answer             jsonb NOT NULL DEFAULT '{}'::jsonb,
    create_by          bigint,
    create_time        timestamptz NOT NULL DEFAULT now(),
    update_by          bigint,
    update_time        timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE question_choice IS '选择题表';
COMMENT ON COLUMN question_choice.question_choice_id IS '主键';
COMMENT ON COLUMN question_choice.question_id IS '题目ID';
COMMENT ON COLUMN question_choice.choice_mode IS 'single-单选 | multiple-多选';
COMMENT ON COLUMN question_choice.options IS '选项JSON';
COMMENT ON COLUMN question_choice.answer IS '答案JSON';
COMMENT ON COLUMN question_choice.create_by IS '创建人';
COMMENT ON COLUMN question_choice.create_time IS '创建时间';
COMMENT ON COLUMN question_choice.update_by IS '更新人';
COMMENT ON COLUMN question_choice.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_question_choice_question ON question_choice (question_id);

-- 填空题
CREATE TABLE question_blank (
    question_blank_id bigint PRIMARY KEY,
    question_id       bigint NOT NULL,
    blank_count       int NOT NULL DEFAULT 1,
    answer            jsonb NOT NULL DEFAULT '{}'::jsonb,
    create_by         bigint,
    create_time       timestamptz NOT NULL DEFAULT now(),
    update_by         bigint,
    update_time       timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE question_blank IS '填空题表';
COMMENT ON COLUMN question_blank.question_blank_id IS '主键';
COMMENT ON COLUMN question_blank.question_id IS '题目ID';
COMMENT ON COLUMN question_blank.blank_count IS '空格数量';
COMMENT ON COLUMN question_blank.answer IS '答案JSON';
COMMENT ON COLUMN question_blank.create_by IS '创建人';
COMMENT ON COLUMN question_blank.create_time IS '创建时间';
COMMENT ON COLUMN question_blank.update_by IS '更新人';
COMMENT ON COLUMN question_blank.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_question_blank_question ON question_blank (question_id);

-- 判断题
CREATE TABLE question_judge (
    question_judge_id bigint PRIMARY KEY,
    question_id       bigint NOT NULL,
    answer            boolean NOT NULL,
    create_by         bigint,
    create_time       timestamptz NOT NULL DEFAULT now(),
    update_by         bigint,
    update_time       timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE question_judge IS '判断题表';
COMMENT ON COLUMN question_judge.question_judge_id IS '主键';
COMMENT ON COLUMN question_judge.question_id IS '题目ID';
COMMENT ON COLUMN question_judge.answer IS 'true-正确 | false-错误';
COMMENT ON COLUMN question_judge.create_by IS '创建人';
COMMENT ON COLUMN question_judge.create_time IS '创建时间';
COMMENT ON COLUMN question_judge.update_by IS '更新人';
COMMENT ON COLUMN question_judge.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_question_judge_question ON question_judge (question_id);

-- 简答题
CREATE TABLE question_short_answer (
    question_short_answer_id bigint PRIMARY KEY,
    question_id              bigint NOT NULL,
    reference_answer         text,
    scoring_points           jsonb,
    create_by                bigint,
    create_time              timestamptz NOT NULL DEFAULT now(),
    update_by                bigint,
    update_time              timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE question_short_answer IS '简答题表';
COMMENT ON COLUMN question_short_answer.question_short_answer_id IS '主键';
COMMENT ON COLUMN question_short_answer.question_id IS '题目ID';
COMMENT ON COLUMN question_short_answer.reference_answer IS '参考答案';
COMMENT ON COLUMN question_short_answer.scoring_points IS '评分要点JSON';
COMMENT ON COLUMN question_short_answer.create_by IS '创建人';
COMMENT ON COLUMN question_short_answer.create_time IS '创建时间';
COMMENT ON COLUMN question_short_answer.update_by IS '更新人';
COMMENT ON COLUMN question_short_answer.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_question_short_answer_question ON question_short_answer (question_id);

-- 大题
CREATE TABLE question_comprehensive (
    question_comprehensive_id bigint PRIMARY KEY,
    question_id               bigint NOT NULL,
    material                  jsonb,
    sub_questions             jsonb NOT NULL DEFAULT '[]'::jsonb,
    reference_answer          text,
    create_by                 bigint,
    create_time               timestamptz NOT NULL DEFAULT now(),
    update_by                 bigint,
    update_time               timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE question_comprehensive IS '大题表';
COMMENT ON COLUMN question_comprehensive.question_comprehensive_id IS '主键';
COMMENT ON COLUMN question_comprehensive.question_id IS '题目ID';
COMMENT ON COLUMN question_comprehensive.material IS '材料/背景JSON';
COMMENT ON COLUMN question_comprehensive.sub_questions IS '小题JSON';
COMMENT ON COLUMN question_comprehensive.reference_answer IS '参考答案';
COMMENT ON COLUMN question_comprehensive.create_by IS '创建人';
COMMENT ON COLUMN question_comprehensive.create_time IS '创建时间';
COMMENT ON COLUMN question_comprehensive.update_by IS '更新人';
COMMENT ON COLUMN question_comprehensive.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_question_comprehensive_question ON question_comprehensive (question_id);

-- 试卷
CREATE TABLE paper (
    paper_id       bigint PRIMARY KEY,
    owner_id       bigint,
    title          varchar(200) NOT NULL,
    description    text,
    subject        varchar(64),
    status         varchar(16) NOT NULL DEFAULT 'draft'
                   CHECK (status IN ('draft', 'published', 'archived')), -- draft-草稿 | published-已发布 | archived-已归档
    total_score    numeric(8,2) NOT NULL DEFAULT 0,
    question_count int NOT NULL DEFAULT 0,
    settings       jsonb NOT NULL DEFAULT '{}'::jsonb,
    create_by      bigint,
    create_time    timestamptz NOT NULL DEFAULT now(),
    update_by      bigint,
    update_time    timestamptz NOT NULL DEFAULT now(),
    published_at   timestamptz
);
COMMENT ON TABLE paper IS '试卷表';
COMMENT ON COLUMN paper.paper_id IS '主键';
COMMENT ON COLUMN paper.owner_id IS '所属用户ID';
COMMENT ON COLUMN paper.title IS '试卷标题';
COMMENT ON COLUMN paper.description IS '试卷说明';
COMMENT ON COLUMN paper.subject IS '学科';
COMMENT ON COLUMN paper.status IS 'draft-草稿 | published-已发布 | archived-已归档';
COMMENT ON COLUMN paper.total_score IS '总分';
COMMENT ON COLUMN paper.question_count IS '题数';
COMMENT ON COLUMN paper.settings IS '组卷配置JSON';
COMMENT ON COLUMN paper.create_by IS '创建人';
COMMENT ON COLUMN paper.create_time IS '创建时间';
COMMENT ON COLUMN paper.update_by IS '更新人';
COMMENT ON COLUMN paper.update_time IS '更新时间';
COMMENT ON COLUMN paper.published_at IS '发布时间';
CREATE INDEX idx_paper_owner_status ON paper (owner_id, status, update_time DESC);

-- 试卷大题（部分）
CREATE TABLE paper_part (
    paper_part_id bigint PRIMARY KEY,
    paper_id      bigint,
    title         varchar(100) NOT NULL,
    instruction   text,
    sort_order    int NOT NULL DEFAULT 0,
    create_by     bigint,
    create_time   timestamptz NOT NULL DEFAULT now(),
    update_by     bigint,
    update_time   timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE paper_part IS '试卷大题（部分）表';
COMMENT ON COLUMN paper_part.paper_part_id IS '主键';
COMMENT ON COLUMN paper_part.paper_id IS '试卷ID';
COMMENT ON COLUMN paper_part.title IS '大题标题';
COMMENT ON COLUMN paper_part.instruction IS '答题说明';
COMMENT ON COLUMN paper_part.sort_order IS '排序号';
COMMENT ON COLUMN paper_part.create_by IS '创建人';
COMMENT ON COLUMN paper_part.create_time IS '创建时间';
COMMENT ON COLUMN paper_part.update_by IS '更新人';
COMMENT ON COLUMN paper_part.update_time IS '更新时间';
CREATE INDEX idx_paper_part_paper ON paper_part (paper_id, sort_order);

-- 试卷题目
CREATE TABLE paper_question (
    paper_question_id bigint PRIMARY KEY,
    paper_id          bigint,
    paper_part_id     bigint,
    question_id       bigint,
    sort_order        int NOT NULL DEFAULT 0,
    score             numeric(6,2) NOT NULL DEFAULT 2,
    create_by         bigint,
    create_time       timestamptz NOT NULL DEFAULT now(),
    update_by         bigint,
    update_time       timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE paper_question IS '试卷题目关联表';
COMMENT ON COLUMN paper_question.paper_question_id IS '主键';
COMMENT ON COLUMN paper_question.paper_id IS '试卷ID';
COMMENT ON COLUMN paper_question.paper_part_id IS '试卷大题ID';
COMMENT ON COLUMN paper_question.question_id IS '题目ID';
COMMENT ON COLUMN paper_question.sort_order IS '排序号';
COMMENT ON COLUMN paper_question.score IS '本题分值';
COMMENT ON COLUMN paper_question.create_by IS '创建人';
COMMENT ON COLUMN paper_question.create_time IS '创建时间';
COMMENT ON COLUMN paper_question.update_by IS '更新人';
COMMENT ON COLUMN paper_question.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_paper_question ON paper_question (paper_id, question_id);
CREATE INDEX idx_paper_question_sort ON paper_question (paper_id, sort_order);

-- 答卷（一次作答）
CREATE TABLE exam_record (
    exam_record_id bigint PRIMARY KEY,
    paper_id       bigint NOT NULL,
    owner_id       bigint NOT NULL,
    status         varchar(16) NOT NULL DEFAULT 'doing'
                   CHECK (status IN ('doing', 'submitted')), -- doing-进行中 | submitted-已提交
    score          numeric(8,2) NOT NULL DEFAULT 0,
    total_score    numeric(8,2) NOT NULL DEFAULT 0,
    started_at     timestamptz,
    submitted_at   timestamptz,
    create_by      bigint,
    create_time    timestamptz NOT NULL DEFAULT now(),
    update_by      bigint,
    update_time    timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE exam_record IS '答卷表（一次作答）';
COMMENT ON COLUMN exam_record.exam_record_id IS '主键';
COMMENT ON COLUMN exam_record.paper_id IS '试卷ID';
COMMENT ON COLUMN exam_record.owner_id IS '作答用户ID';
COMMENT ON COLUMN exam_record.status IS 'doing-进行中 | submitted-已提交';
COMMENT ON COLUMN exam_record.score IS '得分';
COMMENT ON COLUMN exam_record.total_score IS '卷面总分';
COMMENT ON COLUMN exam_record.started_at IS '开始作答时间';
COMMENT ON COLUMN exam_record.submitted_at IS '交卷时间';
COMMENT ON COLUMN exam_record.create_by IS '创建人';
COMMENT ON COLUMN exam_record.create_time IS '创建时间';
COMMENT ON COLUMN exam_record.update_by IS '更新人';
COMMENT ON COLUMN exam_record.update_time IS '更新时间';
CREATE INDEX idx_exam_record_owner ON exam_record (owner_id, create_time DESC);
CREATE INDEX idx_exam_record_paper ON exam_record (paper_id);

-- 答题明细
CREATE TABLE exam_answer (
    exam_answer_id    bigint PRIMARY KEY,
    exam_record_id    bigint NOT NULL,
    paper_question_id bigint NOT NULL,
    question_id       bigint NOT NULL,
    question_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    user_answer       jsonb NOT NULL DEFAULT '{}'::jsonb,
    is_correct        smallint NOT NULL DEFAULT 2,          -- 0-错误 | 1-正确 | 2-待批改
    score             numeric(6,2) NOT NULL DEFAULT 0,
    create_by         bigint,
    create_time       timestamptz NOT NULL DEFAULT now(),
    update_by         bigint,
    update_time       timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE exam_answer IS '答题明细表';
COMMENT ON COLUMN exam_answer.exam_answer_id IS '主键';
COMMENT ON COLUMN exam_answer.exam_record_id IS '答卷ID';
COMMENT ON COLUMN exam_answer.paper_question_id IS '试卷题目ID';
COMMENT ON COLUMN exam_answer.question_id IS '题目ID';
COMMENT ON COLUMN exam_answer.question_snapshot IS '题干/选项/答案快照JSON';
COMMENT ON COLUMN exam_answer.user_answer IS '用户作答JSON';
COMMENT ON COLUMN exam_answer.is_correct IS '0-错误 | 1-正确 | 2-待批改';
COMMENT ON COLUMN exam_answer.score IS '本题得分';
COMMENT ON COLUMN exam_answer.create_by IS '创建人';
COMMENT ON COLUMN exam_answer.create_time IS '创建时间';
COMMENT ON COLUMN exam_answer.update_by IS '更新人';
COMMENT ON COLUMN exam_answer.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_exam_answer ON exam_answer (exam_record_id, paper_question_id);
CREATE INDEX idx_exam_answer_question ON exam_answer (question_id);

-- 错题本
CREATE TABLE wrong_question (
    wrong_question_id bigint PRIMARY KEY,
    owner_id          bigint NOT NULL,
    question_id       bigint NOT NULL,
    paper_id          bigint,
    wrong_count       int NOT NULL DEFAULT 1,
    last_wrong_time   timestamptz,
    mastered          smallint NOT NULL DEFAULT 0,          -- 0-未掌握 | 1-已掌握
    remark            text,
    create_by         bigint,
    create_time       timestamptz NOT NULL DEFAULT now(),
    update_by         bigint,
    update_time       timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE wrong_question IS '错题本表';
COMMENT ON COLUMN wrong_question.wrong_question_id IS '主键';
COMMENT ON COLUMN wrong_question.owner_id IS '所属用户ID';
COMMENT ON COLUMN wrong_question.question_id IS '题目ID';
COMMENT ON COLUMN wrong_question.paper_id IS '来源试卷ID';
COMMENT ON COLUMN wrong_question.wrong_count IS '错误次数';
COMMENT ON COLUMN wrong_question.last_wrong_time IS '最近错误时间';
COMMENT ON COLUMN wrong_question.mastered IS '0-未掌握 | 1-已掌握';
COMMENT ON COLUMN wrong_question.remark IS '错题笔记';
COMMENT ON COLUMN wrong_question.create_by IS '创建人';
COMMENT ON COLUMN wrong_question.create_time IS '创建时间';
COMMENT ON COLUMN wrong_question.update_by IS '更新人';
COMMENT ON COLUMN wrong_question.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_wrong_question ON wrong_question (owner_id, question_id);
CREATE INDEX idx_wrong_question_status ON wrong_question (owner_id, mastered, last_wrong_time DESC);

-- 生成任务
CREATE TABLE generation_task (
    generation_task_id bigint PRIMARY KEY,
    owner_id           bigint,
    document_id        bigint,
    paper_id           bigint,
    task_type          varchar(32) NOT NULL
                       CHECK (task_type IN ('extract', 'generate', 'assemble', 'import')), -- extract-抽题 | generate-AI出题 | assemble-组卷 | import-导入
    status             varchar(16) NOT NULL DEFAULT 'pending'
                       CHECK (status IN ('pending', 'processing', 'succeeded', 'failed')), -- pending-待处理 | processing-处理中 | succeeded-成功 | failed-失败
    progress           smallint NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    params             jsonb NOT NULL DEFAULT '{}'::jsonb,
    result             jsonb,
    error_message      text,
    started_at         timestamptz,
    finished_at        timestamptz,
    create_by          bigint,
    create_time        timestamptz NOT NULL DEFAULT now(),
    update_by          bigint,
    update_time        timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE generation_task IS '生成任务表';
COMMENT ON COLUMN generation_task.generation_task_id IS '主键';
COMMENT ON COLUMN generation_task.owner_id IS '所属用户ID';
COMMENT ON COLUMN generation_task.document_id IS '源文档ID';
COMMENT ON COLUMN generation_task.paper_id IS '试卷ID';
COMMENT ON COLUMN generation_task.task_type IS 'extract-抽题 | generate-AI出题 | assemble-组卷 | import-导入';
COMMENT ON COLUMN generation_task.status IS 'pending-待处理 | processing-处理中 | succeeded-成功 | failed-失败';
COMMENT ON COLUMN generation_task.progress IS '进度0-100';
COMMENT ON COLUMN generation_task.params IS '任务入参JSON';
COMMENT ON COLUMN generation_task.result IS '任务结果JSON';
COMMENT ON COLUMN generation_task.error_message IS '失败原因';
COMMENT ON COLUMN generation_task.started_at IS '开始时间';
COMMENT ON COLUMN generation_task.finished_at IS '结束时间';
COMMENT ON COLUMN generation_task.create_by IS '创建人';
COMMENT ON COLUMN generation_task.create_time IS '创建时间';
COMMENT ON COLUMN generation_task.update_by IS '更新人';
COMMENT ON COLUMN generation_task.update_time IS '更新时间';
CREATE INDEX idx_task_owner_status ON generation_task (owner_id, status, create_time DESC);
CREATE INDEX idx_task_pending ON generation_task (status, create_time) WHERE status IN ('pending', 'processing');
