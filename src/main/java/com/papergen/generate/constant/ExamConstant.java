package com.papergen.generate.constant;

/**
 * 答卷相关常量，与 exam_record.status、exam_answer.is_correct 的取值一致。
 */
public class ExamConstant {

    /** 进行中 */
    public static final String STATUS_DOING = "doing";

    /** 已提交 */
    public static final String STATUS_SUBMITTED = "submitted";

    /** 试卷列表中的用户视角状态：未开始（尚无答卷） */
    public static final String STATUS_NOT_STARTED = "not_started";

    /** 判分结果：错误 */
    public static final int RESULT_WRONG = 0;

    /** 判分结果：正确 */
    public static final int RESULT_CORRECT = 1;

    /** 判分结果：待批改（主观题） */
    public static final int RESULT_PENDING = 2;
}
