package com.papergen.generate.constant;

/**
 * 滑块验证码尺寸与位置常量
 */
public final class CaptchaConstant {

    /**
     * 背景图宽度
     */
    public static final int WIDTH = 320;

    /**
     * 背景图高度
     */
    public static final int HEIGHT = 160;

    /**
     * 拼图块边长
     */
    public static final int PIECE_SIZE = 50;

    /**
     * 缺口最小横坐标，避免贴着左边缘
     */
    public static final int MIN_X = 60;

    /**
     * 缺口最小纵坐标
     */
    public static final int MIN_Y = 20;

    /**
     * 缺口与图片边缘的最小留白
     */
    public static final int MARGIN = 10;

    private CaptchaConstant() {
    }
}
