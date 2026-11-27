-- ============================================================
-- 试卷助手（papergen）Schema — PostgreSQL 16
--
-- 约定：
-- 1. 主键 bigint，MyBatis-Plus 雪花ID，命名 <表名>_id。
-- 2. 公共字段 create_by / create_time / update_by / update_time，AutoHandler 自动填充。
-- 3. 时间 timestamptz；jsonb 存结构多变内容；主表软删，关联表物理删。
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
    deleted_at    timestamptz,
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
COMMENT ON COLUMN sys_user.deleted_at IS '软删时间';
COMMENT ON COLUMN sys_user.create_by IS '创建人';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_by IS '更新人';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username) WHERE deleted_at IS NULL;

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
    deleted_at         timestamptz,
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
COMMENT ON COLUMN source_document.deleted_at IS '软删时间';
COMMENT ON COLUMN source_document.create_by IS '创建人';
COMMENT ON COLUMN source_document.create_time IS '创建时间';
COMMENT ON COLUMN source_document.update_by IS '更新人';
COMMENT ON COLUMN source_document.update_time IS '更新时间';
CREATE INDEX idx_doc_owner ON source_document (owner_id, create_time DESC) WHERE deleted_at IS NULL;

-- 题库
CREATE TABLE question (
    question_id        bigint PRIMARY KEY,
    owner_id           bigint,
    source_document_id bigint,
    type               varchar(24) NOT NULL
                       CHECK (type IN ('single_choice', 'multiple_choice',
                                       'true_false', 'blank',
                                       'short_answer', 'comprehensive')), -- single_choice-单选 | multiple_choice-多选 | true_false-判断 | blank-填空 | short_answer-简答 | comprehensive-综合
    difficulty         varchar(16) NOT NULL DEFAULT 'medium'
                       CHECK (difficulty IN ('easy', 'medium', 'hard')), -- easy-简单 | medium-中等 | hard-困难
    stem               jsonb NOT NULL DEFAULT '{}'::jsonb,
    options            jsonb,
    answer             jsonb NOT NULL DEFAULT '{}'::jsonb,
    analysis           text,
    stem_text          text,
    default_score      numeric(6,2) DEFAULT 2,
    status             varchar(16) NOT NULL DEFAULT 'draft'
                       CHECK (status IN ('draft', 'active', 'archived')), -- draft-草稿 | active-启用 | archived-归档
    deleted_at         timestamptz,
    create_by          bigint,
    create_time        timestamptz NOT NULL DEFAULT now(),
    update_by          bigint,
    update_time        timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE question IS '题库表';
COMMENT ON COLUMN question.question_id IS '主键';
COMMENT ON COLUMN question.owner_id IS '所属用户ID';
COMMENT ON COLUMN question.source_document_id IS '来源文档ID';
COMMENT ON COLUMN question.type IS 'single_choice-单选 | multiple_choice-多选 | true_false-判断 | blank-填空 | short_answer-简答 | comprehensive-综合';
COMMENT ON COLUMN question.difficulty IS 'easy-简单 | medium-中等 | hard-困难';
COMMENT ON COLUMN question.stem IS '题干JSON';
COMMENT ON COLUMN question.options IS '选项JSON';
COMMENT ON COLUMN question.answer IS '答案JSON';
COMMENT ON COLUMN question.analysis IS '解析';
COMMENT ON COLUMN question.stem_text IS '题干明文';
COMMENT ON COLUMN question.default_score IS '默认分值';
COMMENT ON COLUMN question.status IS 'draft-草稿 | active-启用 | archived-归档';
COMMENT ON COLUMN question.deleted_at IS '软删时间';
COMMENT ON COLUMN question.create_by IS '创建人';
COMMENT ON COLUMN question.create_time IS '创建时间';
COMMENT ON COLUMN question.update_by IS '更新人';
COMMENT ON COLUMN question.update_time IS '更新时间';
CREATE INDEX idx_question_owner_status ON question (owner_id, status, create_time DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_question_type_diff ON question (type, difficulty);
CREATE INDEX idx_question_stem_trgm ON question USING gin (stem_text gin_trgm_ops);

-- 标签
CREATE TABLE tag (
    tag_id      bigint PRIMARY KEY,
    owner_id    bigint,
    name        varchar(64) NOT NULL,
    category    varchar(32) NOT NULL DEFAULT 'knowledge',        -- knowledge-知识点 | topic-主题
    create_by   bigint,
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by   bigint,
    update_time timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE tag IS '标签表';
COMMENT ON COLUMN tag.tag_id IS '主键';
COMMENT ON COLUMN tag.owner_id IS '所属用户ID';
COMMENT ON COLUMN tag.name IS '标签名称';
COMMENT ON COLUMN tag.category IS 'knowledge-知识点 | topic-主题';
COMMENT ON COLUMN tag.create_by IS '创建人';
COMMENT ON COLUMN tag.create_time IS '创建时间';
COMMENT ON COLUMN tag.update_by IS '更新人';
COMMENT ON COLUMN tag.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_tag_name ON tag (owner_id, category, name);

-- 题目-标签
CREATE TABLE question_tag (
    question_id bigint NOT NULL,
    tag_id      bigint NOT NULL,
    create_by   bigint,
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by   bigint,
    update_time timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (question_id, tag_id)
);
COMMENT ON TABLE question_tag IS '题目标签关联表';
COMMENT ON COLUMN question_tag.question_id IS '题目ID';
COMMENT ON COLUMN question_tag.tag_id IS '标签ID';
COMMENT ON COLUMN question_tag.create_by IS '创建人';
COMMENT ON COLUMN question_tag.create_time IS '创建时间';
COMMENT ON COLUMN question_tag.update_by IS '更新人';
COMMENT ON COLUMN question_tag.update_time IS '更新时间';
CREATE INDEX idx_question_tag_tag ON question_tag (tag_id);

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
    deleted_at     timestamptz,
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
COMMENT ON COLUMN paper.deleted_at IS '软删时间';
COMMENT ON COLUMN paper.create_by IS '创建人';
COMMENT ON COLUMN paper.create_time IS '创建时间';
COMMENT ON COLUMN paper.update_by IS '更新人';
COMMENT ON COLUMN paper.update_time IS '更新时间';
COMMENT ON COLUMN paper.published_at IS '发布时间';
CREATE INDEX idx_paper_owner_status ON paper (owner_id, status, update_time DESC) WHERE deleted_at IS NULL;

-- 试卷大题
CREATE TABLE paper_section (
    paper_section_id bigint PRIMARY KEY,
    paper_id         bigint,
    title            varchar(100) NOT NULL,
    instruction      text,
    sort_order       int NOT NULL DEFAULT 0,
    create_by        bigint,
    create_time      timestamptz NOT NULL DEFAULT now(),
    update_by        bigint,
    update_time      timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE paper_section IS '试卷大题表';
COMMENT ON COLUMN paper_section.paper_section_id IS '主键';
COMMENT ON COLUMN paper_section.paper_id IS '试卷ID';
COMMENT ON COLUMN paper_section.title IS '大题标题';
COMMENT ON COLUMN paper_section.instruction IS '答题说明';
COMMENT ON COLUMN paper_section.sort_order IS '排序号';
COMMENT ON COLUMN paper_section.create_by IS '创建人';
COMMENT ON COLUMN paper_section.create_time IS '创建时间';
COMMENT ON COLUMN paper_section.update_by IS '更新人';
COMMENT ON COLUMN paper_section.update_time IS '更新时间';
CREATE INDEX idx_paper_section_paper ON paper_section (paper_id, sort_order);

-- 试卷题目
CREATE TABLE paper_question (
    paper_question_id bigint PRIMARY KEY,
    paper_id          bigint,
    section_id        bigint,
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
COMMENT ON COLUMN paper_question.section_id IS '大题ID';
COMMENT ON COLUMN paper_question.question_id IS '题目ID';
COMMENT ON COLUMN paper_question.sort_order IS '排序号';
COMMENT ON COLUMN paper_question.score IS '本题分值';
COMMENT ON COLUMN paper_question.create_by IS '创建人';
COMMENT ON COLUMN paper_question.create_time IS '创建时间';
COMMENT ON COLUMN paper_question.update_by IS '更新人';
COMMENT ON COLUMN paper_question.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_paper_question ON paper_question (paper_id, question_id);
CREATE INDEX idx_paper_question_sort ON paper_question (paper_id, sort_order);

-- 生成任务
CREATE TABLE generation_task (
    generation_task_id bigint PRIMARY KEY,
    owner_id           bigint,
    document_id        bigint,
    paper_id           bigint,
    task_type          varchar(32) NOT NULL
                       CHECK (task_type IN ('extract', 'generate', 'assemble', 'export')), -- extract-抽题 | generate-AI出题 | assemble-组卷 | export-导出
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
COMMENT ON COLUMN generation_task.task_type IS 'extract-抽题 | generate-AI出题 | assemble-组卷 | export-导出';
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
